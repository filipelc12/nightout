package com.nightout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Liga/desliga o "Iniciar com o Windows" gravando o caminho do executavel em
 * HKCU\Software\Microsoft\Windows\CurrentVersion\Run - a mesma chave que a aba
 * "Aplicativos de inicializacao" do Windows lista. HKCU nao exige admin.
 *
 * So funciona na versao instalada (empacotada com jpackage): rodando via
 * "java -jar" nao existe um NightOut.exe estavel para apontar.
 */
public final class WindowsStartupManager {

    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "NightOut";

    /** Caminho do NightOut.exe; o launcher do jpackage define essa propriedade. */
    public Optional<String> executablePath() {
        return Optional.ofNullable(System.getProperty("jpackage.app-path"));
    }

    public boolean isAvailable() {
        return executablePath().isPresent();
    }

    /** Ligado = a chave Run aponta para o executavel atual (nao para uma instalacao antiga). */
    public boolean isEnabled() throws IOException, InterruptedException {
        Optional<String> exe = executablePath();
        if (exe.isEmpty()) {
            return false;
        }
        String output = run("reg", "query", RUN_KEY, "/v", VALUE_NAME);
        return output.toLowerCase().contains(exe.get().toLowerCase());
    }

    public void enable() throws IOException, InterruptedException {
        String exe = executablePath()
                .orElseThrow(() -> new IOException("Iniciar com o Windows so funciona na versao instalada"));
        // Aspas em volta do caminho: a pasta de instalacao pode ter espacos.
        runOrFail("reg", "add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\"" + exe + "\"", "/f");
        Logger.log("Iniciar com o Windows ativado (" + exe + ")");
    }

    public void disable() throws IOException, InterruptedException {
        // reg delete falha se o valor nao existir; nesse caso ja esta desligado.
        run("reg", "delete", RUN_KEY, "/v", VALUE_NAME, "/f");
        Logger.log("Iniciar com o Windows desativado");
    }

    private void runOrFail(String... command) throws IOException, InterruptedException {
        Process process = start(command);
        readAll(process);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("Falha ao executar " + command[0] + " (exit code " + exit + ")");
        }
    }

    private String run(String... command) throws IOException, InterruptedException {
        Process process = start(command);
        String output = readAll(process);
        process.waitFor();
        return output;
    }

    private Process start(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
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
