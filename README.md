# NightOut

> **Status: Beta.** Funcional, mas ainda em ajustes - use por sua conta e risco.

Troca automaticamente o tema (claro/escuro) do Windows com base no nascer e no
por do sol da cidade configurada.

- Se a hora atual >= por do sol -> tema escuro
- Se a hora atual >= nascer do sol (e ainda nao passou do por do sol) -> tema claro
- Caso contrario (antes do nascer do sol) -> tema escuro

O app fica residente na bandeja do Windows, verificando o horario a cada
alguns minutos (configuravel) e reaplicando o tema quando necessario.

## Requisitos

- Java 21 (Microsoft Build of OpenJDK)
- Maven 3.9+
- Windows 10/11

## Build

```
mvn clean package
```

Gera `target/nightout.jar`. Use sempre `clean package` (nao so `package`) -
sem o `clean`, builds incrementais as vezes empacotam `.class` desatualizado
neste ambiente.

## Rodar

```
java -jar target/nightout.jar
```

Na primeira execucao, uma janela pede pra buscar e escolher a cidade (usa a
API publica de geocoding do Open-Meteo). Depois disso o app roda na bandeja;
clique com o botao direito no icone para:

- **Verificar agora** - forca uma checagem imediata do tema
- **Selecionar cidade...** - troca a cidade usada para calcular o sol
- **Sair**

A configuracao fica salva em `%APPDATA%\NightOut\config.properties` e o log
em `%APPDATA%\NightOut\nightout.log`.

## Como funciona

- **Geocoding**: [Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api) (gratuita, sem chave)
- **Nascer/por do sol**: [sunrise-sunset.org API](https://sunrise-sunset.org/api) (gratuita, sem chave)
- **Tema do Windows**: leitura/escrita do registro
  `HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`
  (chaves `AppsUseLightTheme` e `SystemUsesLightTheme`) via `reg.exe`, seguido
  de um broadcast `WM_SETTINGCHANGE` (via P/Invoke de `user32.dll` chamado
  por PowerShell) pra Explorer/barra de tarefas atualizarem na hora

## Contribuindo

Nada vai direto pra `main`: toda mudanca sai numa branch, vira Pull Request e
so e mesclada apos aprovacao manual no GitHub.
