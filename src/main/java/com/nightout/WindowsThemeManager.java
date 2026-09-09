package com.nightout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Le e altera o tema claro/escuro do Windows via registro
 * (HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize),
 * chamando reg.exe. Nao requer nenhuma dependencia nativa (JNA/JNI).
 */
public final class WindowsThemeManager {

    public enum Theme { LIGHT, DARK }

    private static final String REG_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

    public Theme getCurrentTheme() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "reg", "query", REG_KEY, "/v", "AppsUseLightTheme");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readAll(process);
        process.waitFor();
        // saida esperada contem algo como: AppsUseLightTheme    REG_DWORD    0x1
        if (output.contains("0x1")) {
            return Theme.LIGHT;
        }
        return Theme.DARK;
    }

    public void setTheme(Theme theme) throws IOException, InterruptedException {
        int value = theme == Theme.LIGHT ? 1 : 0;
        setValue("AppsUseLightTheme", value);
        setValue("SystemUsesLightTheme", value);
        Logger.log("Tema do Windows alterado para " + theme);
    }

    private void setValue(String name, int value) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "reg", "add", REG_KEY, "/v", name, "/t", "REG_DWORD", "/d", String.valueOf(value), "/f");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        readAll(process);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("Falha ao definir " + name + " (exit code " + exit + ")");
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
