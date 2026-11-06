---
name: arquitetura-do-sistema
description: Arquitetura, convencoes de codigo e padroes de clean code do NightOut (app Java/Swing de bandeja que troca tema e wallpaper do Windows pelo sol). Use SEMPRE antes de criar, alterar ou refatorar qualquer codigo deste projeto.
---

# Arquitetura do sistema - NightOut

App desktop Java 21 (Maven, sem dependencias externas em runtime) que fica na
bandeja do Windows e alterna tema claro/escuro + papel de parede conforme o
nascer/por do sol da cidade configurada.

## Mapa das classes (`src/main/java/com/nightout`)

| Classe | Papel |
|---|---|
| `App` | Ponto de entrada. Carrega config, pede cidade na 1a execucao, monta o icone/menu da bandeja e liga o `ThemeScheduler`. |
| `AppConfig` | Config persistida em `%APPDATA%\NightOut\config.properties` (cidade, lat/lon, intervalo, caminhos de wallpaper, modo do tema). Campos publicos + `load()`/`save()`. |
| `ThemeScheduler` | Thread unica (`nightout-scheduler`, daemon) que roda `check()` a cada `intervalMinutes`. Decide o tema, aplica tema e wallpaper e avisa a UI via `Consumer<Theme>`. Faz cache do sol de hoje/amanha. |
| `WindowsThemeManager` | Le/escreve `AppsUseLightTheme`/`SystemUsesLightTheme` via `reg.exe` e faz broadcast `WM_SETTINGCHANGE` via PowerShell. Define o enum `Theme { LIGHT, DARK }`. |
| `ThemeMode` | Enum `AUTO` / `LIGHT` / `DARK`: automatico pelo sol ou tema fixo (manual). Salvo como `theme.mode`. |
| `WindowsStartupManager` | Liga/desliga o "Iniciar com o Windows" (valor `NightOut` em `HKCU\...\CurrentVersion\Run`) via `reg.exe`. So disponivel no app empacotado: usa a propriedade `jpackage.app-path`. |
| `WallpaperManager` | Troca wallpaper com `SystemParametersInfo` chamado por script PowerShell. |
| `TrayIconFactory` | Desenha o icone (sol/lua) em `BufferedImage`. |
| `SettingsDialog` | Dialogo Swing de busca/selecao de cidade (busca em `SwingWorker`). |
| `WallpaperSettingsDialog` | Dialogo Swing para escolher as imagens de dia/noite. |
| `Logger` | Log simples em `%APPDATA%\NightOut\nightout.log`. |
| `CityResult`, `SunTimes`, `CheckResult` | `record`s de dados. `CheckResult` e o retorno de uma checagem (tema + proximos nascer/por do sol; horarios `null` no modo manual). |
| `client/GeocodingService` | API Open-Meteo Geocoding (busca de cidade). |
| `client/SunTimesService` | API sunrise-sunset.org (nascer/por do sol por data). |
| `client/Json` | Parser JSON minimo proprio (nao adicionar lib de JSON). |

## Fluxo principal

1. `App.main` -> checa `SystemTray.isSupported()` -> `SwingUtilities.invokeLater(App::start)`.
2. `start()` carrega `AppConfig`; sem cidade, abre `SettingsDialog`.
3. Cria `ThemeScheduler`, monta a bandeja (`criarIconeDaBandeja`) e chama `scheduler.start()`.
4. A cada ciclo, `ThemeScheduler.check()`: se o modo for manual, aplica o tema fixo
   e para por ai; senao busca o sol (com cache), escolhe o tema,
   aplica tema/wallpaper se mudou e chama `App.onThemeApplied`, que atualiza
   icone/tooltip **dentro do EDT** (`SwingUtilities.invokeLater`).

## Regras de arquitetura

- **Threads**: tudo de Swing/AWT no EDT; tudo que e lento (HTTP, `reg.exe`,
  PowerShell) no executor do `ThemeScheduler` ou em `SwingWorker`. Acoes do menu
  que precisam de checagem chamam `scheduler.checkNow()`, nunca `check()` direto.
  Para mostrar o resultado ao usuario, use `scheduler.checkNow(onResult, onError)`:
  os callbacks rodam na thread do scheduler, entao volte ao EDT com
  `SwingUtilities.invokeLater` antes de mexer na UI. Avisos ao usuario usam
  `TrayIcon.displayMessage` (metodo `notificar` em `App`).
- **Integracao com o Windows**: sem JNI/JNA. Usar `reg.exe` ou script PowerShell
  via `ProcessBuilder`, seguindo o padrao de `WindowsThemeManager`/`WallpaperManager`.
- **Chamadas HTTP** ficam no pacote `client`, usando `java.net.http.HttpClient` e `Json`.
- **Sem dependencias novas** de runtime no `pom.xml` sem combinar antes (JUnit so em `test`).
- **Erros**: capturar e registrar com `Logger.log(...)`; o app residente nao pode cair
  por erro de rede/registro.
- **Config nova** entra em `AppConfig` (campo + `fromProperties` + `save`) e, se for
  editavel, em um dialogo proprio.

## Convencoes de clean code

- **Metodos pequenos, um proposito cada.** Metodo que monta varias coisas apenas
  orquestra; cada parte vira um metodo proprio.
- **Itens de menu da bandeja**: cada item e criado por um metodo
  `criar<NomeDoBotao>()` que devolve o `MenuItem` ja com seu listener.
  `criarIconeDaBandeja()` so faz `popup.add(criarX())` em ordem. Ex.:
  `criarVerificarAgora()`, `criarSelecionarCidade()`,
  `criarConfigurarPapeisDeParede()`, `criarSair(tray)`. Botao novo = metodo novo
  no mesmo padrao. Submenus seguem a mesma ideia: `criarMenuTema()` so monta
  `criarAutomatico()`, `criarClaro()`, `criarEscuro()`.
- **Idioma**: textos de UI e logs em portugues **sem acento** (padrao atual do
  projeto). Codigo novo pode ter nomes em portugues (`criar...`, `iconeBandeja`);
  ao mexer em codigo existente, siga o idioma do entorno - nao renomeie em massa
  sem pedido.
- Comentarios curtos e so quando explicam o *porque* (ver `ThemeScheduler`).
- Classes utilitarias: `final` + construtor privado. Dados imutaveis: `record`.

## Build e verificacao

```
mvn package          # gera target/nightout.jar
mvn -q -o compile    # checagem rapida de compilacao
java -jar target/nightout.jar
```

Versao fica em `pom.xml` (`<version>`); o README lista os itens do menu da
bandeja - atualize-o ao adicionar/remover botao.

## Empacotamento (app sem precisar de Java)

- `packaging/build-installer.ps1` roda `mvn package` + `jpackage` e gera em
  `target/dist`: o app (`NightOut/NightOut.exe` + runtime), o zip portatil e,
  se o WiX 3 estiver no PATH, o `.msi` (instalacao por usuario).
- O runtime e montado pelo jlink so com os modulos da variavel `$modulos` do
  script. **Se o codigo passar a usar um modulo novo do JDK** (ex.: `java.sql`,
  `java.logging`), rode `jdeps --print-module-deps target/nightout.jar` e
  atualize `$modulos`, senao o app empacotado quebra (via `java -jar` funciona,
  o que esconde o problema).
- `$upgradeUuid` do script e fixo: nunca altere (e o que faz um `.msi` novo
  atualizar o anterior).
- Icone: `packaging/NightOut.ico`, gerado por `packaging/GerarIcone.java`.
- Teste em Windows limpo: `packaging/testar-na-sandbox.wsb` (Windows Sandbox).
- Codigo que depende de estar empacotado (ex.: `WindowsStartupManager`) deve
  checar `System.getProperty("jpackage.app-path")` e degradar com elegancia
  quando rodar via `java -jar`.
