# NightOut

Troca automaticamente o tema (claro/escuro) do Windows com base no nascer e no
por do sol da cidade configurada.

- Se a hora atual >= por do sol -> tema escuro
- Se a hora atual >= nascer do sol (e ainda nao passou do por do sol) -> tema claro
- Caso contrario (antes do nascer do sol) -> tema escuro

O app fica residente na bandeja do Windows, verificando o horario a cada
alguns minutos (configuravel) e reaplicando o tema quando necessario.

## Baixar e usar no Windows

[**Baixar NightOut para Windows — ZIP portatil**](https://github.com/filipelc12/nightout/releases/download/v1.1.0/NightOut-1.1.0-portatil.zip)

**Nao precisa instalar Java nem Maven.** O ZIP ja inclui o aplicativo e o
runtime Java. E necessario acesso a internet para buscar a cidade e os horarios
do sol.

### Como fazer o download e abrir

1. Clique em **Baixar NightOut para Windows** acima e aguarde o download de
   `NightOut-1.1.0-portatil.zip` terminar.
2. Na pasta **Downloads**, clique com o botao direito no ZIP e escolha
   **Extrair Tudo...**. Selecione uma pasta onde deseja manter o aplicativo.
3. Abra a pasta extraida e, dentro dela, a pasta `NightOut`.
4. De um duplo clique em `NightOut.exe`. Mantenha as pastas `app` e `runtime`
   junto do executavel; elas sao necessarias para abrir o programa.
5. Na primeira execucao, busque sua cidade e selecione o resultado correto.
6. O NightOut fica na bandeja do Windows, perto do relogio. Se o icone nao
   estiver visivel, abra a seta de icones ocultos. Clique com o botao direito
   nele para acessar as opcoes de tema, cidade e papeis de parede.

Extraia o ZIP antes de abrir o executavel. A versao portatil nao precisa de
instalador. A release `v1.1.0` disponibiliza o ZIP; o MSI e opcional no processo
de empacotamento descrito abaixo.

O executavel nao e assinado digitalmente. Se o Windows mostrar **O Windows
protegeu o computador**, confira se o arquivo veio desta pagina oficial do
projeto antes de escolher **Mais informacoes -> Executar assim mesmo**.

Voce tambem pode acessar a [pagina da release v1.1.0](https://github.com/filipelc12/nightout/releases/tag/v1.1.0)
e baixar `NightOut-1.1.0-portatil.zip` na secao **Assets**. Os arquivos
**Source code (zip)** e **Source code (tar.gz)** contem o codigo-fonte; para
usar o aplicativo pronto, escolha o ZIP portatil.

### Iniciar com o Windows, atualizar e remover

- Para abrir automaticamente ao entrar no Windows, marque **Iniciar com o
  Windows** no menu da bandeja. Mantenha a pasta do aplicativo no mesmo local.
  Se mover a pasta, desmarque a opcao antes e ative-a novamente no novo local.
- Para atualizar, desmarque **Iniciar com o Windows**, se estiver ativo, e
  escolha **Sair** no menu da bandeja. Baixe e extraia a nova versao, abra o
  novo `NightOut.exe` e reative a inicializacao automatica se desejar. Suas
  configuracoes ficam em `%APPDATA%\NightOut` e sao preservadas.
- Para remover a versao portatil, desmarque **Iniciar com o Windows**, escolha
  **Sair** e apague a pasta extraida. Se quiser remover tambem as preferencias
  e os logs, apague a pasta `%APPDATA%\NightOut`.

## Requisitos (para desenvolver)

- Java 21 (Microsoft Build of OpenJDK)
- Maven 3.9+
- Windows 10/11

## Build

```
mvn package
```

Gera `target/nightout.jar`.

## Rodar

```
java -jar target/nightout.jar
```

Na primeira execucao, uma janela pede pra buscar e escolher a cidade (usa a
API publica de geocoding do Open-Meteo). Depois disso o app roda na bandeja;
clique com o botao direito no icone para:

- **Tema** - escolhe como o tema e definido:
  - **Automatico (pelo sol)** - comportamento padrao, troca no nascer/por do sol
  - **Claro (manual)** / **Escuro (manual)** - fixa o tema escolhido (e o
    wallpaper correspondente) ate voltar para o automatico. A escolha fica
    salva e vale tambem depois de reiniciar o app.
- **Verificar agora** - forca uma checagem imediata do tema e mostra uma
  notificacao com o resultado: no tema claro, o horario do proximo por do
  sol; no tema escuro, o do proximo nascer do sol (no modo manual, avisa que
  o tema esta fixo)
- **Selecionar cidade...** - troca a cidade usada para calcular o sol
- **Configurar papeis de parede...** - define os arquivos de imagem usados
  como wallpaper no tema claro (dia) e no tema escuro (noite); ao trocar de
  tema, o wallpaper correspondente e aplicado automaticamente. Deixar um dos
  campos em branco desativa a troca de wallpaper para aquele tema.
- **Iniciar com o Windows** - abre o NightOut automaticamente ao entrar no
  Windows (so no app empacotado; rodando via `java -jar` fica desabilitado)
- **Sair**

A configuracao fica salva em `%APPDATA%\NightOut\config.properties` e o log
em `%APPDATA%\NightOut\nightout.log`.

## Gerar o pacote para distribuicao

```
powershell -ExecutionPolicy Bypass -File packaging\build-installer.ps1
```

Usa o `jpackage` do JDK 21 para juntar o jar com um runtime Java enxuto e gera
em `target\dist`:

- `NightOut\` - o app pronto (`NightOut.exe` + runtime)
- `NightOut-<versao>-portatil.zip` - o mesmo app zipado
- `NightOut-<versao>.msi` - instalador por usuario (sem admin), com atalho no
  Menu Iniciar. So e gerado se o [WiX Toolset 3](https://wixtoolset.org/)
  estiver no PATH (`winget install WiXToolset.WiXToolset`)

A versao vem do `<version>` do `pom.xml`. O icone e o `packaging\NightOut.ico`,
gerado por `packaging\GerarIcone.java`.

Para testar num Windows limpo (sem Java), ative a **Area Restrita do Windows**
(Windows Sandbox) e de um duplo clique em `packaging\testar-na-sandbox.wsb`:
ela abre com a pasta `target\dist` compartilhada.

## Como funciona

- **Geocoding**: [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api) (gratuita, sem chave)
- **Nascer/por do sol**: [sunrise-sunset.org API](https://sunrise-sunset.org/api) (gratuita, sem chave)
- **Tema do Windows**: leitura/escrita do registro
  `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
  (chaves `AppsUseLightTheme` e `SystemUsesLightTheme`) via `reg.exe`
- **Wallpaper**: troca via API nativa `SystemParametersInfo`
  (`SPI_SETDESKWALLPAPER`), chamada por um script PowerShell (mesmo padrao
  sem dependencia nativa usado na troca de tema)
- **Iniciar com o Windows**: valor `NightOut` em
  `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` apontando para o
  `NightOut.exe` (o mesmo lugar que a aba "Aplicativos de inicializacao" do
  Windows lista)
