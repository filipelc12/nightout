# NightOut

Troca automaticamente o tema (claro/escuro) do Windows com base no nascer e no
por do sol da cidade configurada.

- Se a hora atual >= por do sol -> tema escuro
- Se a hora atual >= nascer do sol (e ainda nao passou do por do sol) -> tema claro
- Caso contrario (antes do nascer do sol) -> tema escuro

O app fica residente na bandeja do Windows, verificando o horario a cada
alguns minutos (configuravel) e reaplicando o tema quando necessario.

## Para quem so quer usar

[**Baixar NightOut para Windows — ZIP portatil**](https://github.com/filipelc12/nightout/releases/download/v1.1.0/NightOut-1.1.0-portatil.zip)

O link baixa diretamente o aplicativo com Java embutido. Extraia o ZIP e abra
`NightOut\NightOut.exe`.

Baixe a versao portatil (`NightOut-<versao>-portatil.zip`) ou o instalador
(`NightOut-<versao>.msi`). **Nao precisa ter Java instalado**: o app ja leva
um runtime Java enxuto embutido. Na versao portatil, descompacte e abra o
`NightOut.exe`.

Como o executavel nao e assinado digitalmente, na primeira vez o Windows pode
mostrar "O Windows protegeu o computador": clique em **Mais informacoes ->
Executar assim mesmo**.

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
