package com.nightout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Troca o papel de parede da area de trabalho do Windows chamando a API
 * nativa SystemParametersInfo (SPI_SETDESKWALLPAPER) via um P/Invoke
 * disparado por um script PowerShell - mesma abordagem sem dependencia
 * nativa Java/JNA usada em WindowsThemeManager.
 */
public final class WallpaperManager {

    public void setWallpaper(Path imagePath) throws IOException, InterruptedException {
        if (imagePath == null) {
            throw new IOException("Caminho de wallpaper nao informado.");
        }
        if (!Files.exists(imagePath)) {
            throw new IOException("Arquivo de wallpaper nao encontrado: " + imagePath);
        }

        // Script escrito num .ps1 temporario (em vez de -Command) pra evitar
        // problemas de quoting do Windows; o caminho da imagem e passado como
        // parametro do script, nao interpolado no C#.
        String script = """
                param([string]$Path)
                Add-Type -Namespace NightOut -Name Wallpaper -MemberDefinition '
                    [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
                    public static extern int SystemParametersInfo(int uAction, int uParam, string lpvParam, int fuWinIni);
                ';
                $SPI_SETDESKWALLPAPER = 0x0014
                $SPIF_UPDATEINIFILE = 0x01
                $SPIF_SENDCHANGE = 0x02
                [NightOut.Wallpaper]::SystemParametersInfo($SPI_SETDESKWALLPAPER, 0, $Path, ($SPIF_UPDATEINIFILE -bor $SPIF_SENDCHANGE)) | Out-Null
                """;
        Path scriptFile = Files.createTempFile("nightout-wallpaper", ".ps1");
        try {
            Files.writeString(scriptFile, script, StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.toAbsolutePath().toString(),
                    "-Path", imagePath.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readAll(process);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IOException("Falha ao definir wallpaper (exit code " + exit + "): " + output.trim());
            }
            Logger.log("Wallpaper alterado para " + imagePath);
        } finally {
            try {
                Files.deleteIfExists(scriptFile);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private String readAll(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
