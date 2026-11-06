<#
Gera o NightOut para distribuicao, rodando sem Java instalado na maquina do usuario.

O jpackage (vem com o JDK 21) junta o nightout.jar com um runtime Java enxuto
(montado pelo jlink so com os modulos que o app usa) e gera:

  target\dist\NightOut\                      -> app pronto (NightOut.exe + runtime)
  target\dist\NightOut-<versao>-portatil.zip -> o mesmo app zipado, sem instalacao
  target\dist\NightOut-<versao>.msi          -> instalador (so se o WiX 3 estiver instalado)

Uso (na raiz do projeto):
  powershell -ExecutionPolicy Bypass -File packaging\build-installer.ps1

Requisitos: JDK 21 (JAVA_HOME ou java no PATH) e Maven. Para o .msi, o
WiX Toolset 3.x no PATH (winget install WiXToolset.WiXToolset).
#>
$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

# Modulos do runtime: "jdeps --print-module-deps target\nightout.jar" lista os
# tres primeiros; jdk.crypto.ec garante HTTPS (TLS com curvas elipticas).
$modulos = 'java.base,java.desktop,java.net.http,jdk.crypto.ec'

# Fixo para sempre: e o que faz o Windows tratar um .msi novo como atualizacao
# do anterior (substitui em vez de instalar duas vezes). Nao altere.
$upgradeUuid = '45c66b66-c9e2-496b-8404-83c256f23d2c'

$versao = ([xml](Get-Content pom.xml)).project.version
$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { Split-Path -Parent (Split-Path -Parent (Get-Command java).Source) }
$jpackage = Join-Path $jdk 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) {
    throw "jpackage nao encontrado em $jpackage. Defina JAVA_HOME apontando para um JDK 21."
}

$entrada = 'target\jpackage-input'
$dist = 'target\dist'

Write-Host "== NightOut $versao =="

Write-Host '-> Compilando o jar (mvn package)'
mvn -q package
if ($LASTEXITCODE -ne 0) { throw 'mvn package falhou' }

# O jpackage copia tudo que estiver na pasta de entrada, entao ela so tem o jar.
Remove-Item -Recurse -Force $entrada, $dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $entrada, $dist | Out-Null
Copy-Item target\nightout.jar $entrada

Write-Host '-> Gerando o app com runtime Java embutido (jpackage --type app-image)'
& $jpackage --type app-image `
    --name NightOut `
    --app-version $versao `
    --vendor NightOut `
    --description 'Troca o tema do Windows pelo nascer e por do sol' `
    --icon packaging\NightOut.ico `
    --input $entrada `
    --main-jar nightout.jar `
    --main-class com.nightout.App `
    --add-modules $modulos `
    --jlink-options '--strip-debug --no-header-files --no-man-pages --strip-native-commands' `
    --dest $dist
if ($LASTEXITCODE -ne 0) { throw 'jpackage (app-image) falhou' }

$zip = Join-Path $dist "NightOut-$versao-portatil.zip"
Write-Host "-> Compactando versao portatil ($zip)"
Compress-Archive -Path (Join-Path $dist 'NightOut') -DestinationPath $zip

if (Get-Command candle.exe -ErrorAction SilentlyContinue) {
    Write-Host '-> Gerando instalador .msi'
    & $jpackage --type msi `
        --app-image (Join-Path $dist 'NightOut') `
        --name NightOut `
        --app-version $versao `
        --vendor NightOut `
        --description 'Troca o tema do Windows pelo nascer e por do sol' `
        --win-per-user-install `
        --win-menu `
        --win-menu-group NightOut `
        --win-upgrade-uuid $upgradeUuid `
        --dest $dist
    if ($LASTEXITCODE -ne 0) { throw 'jpackage (msi) falhou' }
} else {
    Write-Warning 'WiX Toolset 3 nao encontrado no PATH: pulando o .msi (a versao portatil foi gerada).'
}

Write-Host ''
Write-Host 'Pronto. Arquivos em target\dist:'
Get-ChildItem $dist -File | ForEach-Object { '  {0}  ({1:N1} MB)' -f $_.Name, ($_.Length / 1MB) }
