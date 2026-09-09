package com.nightout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Le e altera o tema claro/escuro do Windows via registro
 * (HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize),
 * chamando reg.exe. Nao requer nenhuma dependencia nativa (JNA/JNI).
 *
 * Escrever no registro sozinho nao atualiza a barra de tarefas/Explorer na
 * hora - e preciso avisar o Windows que uma configuracao mudou, do mesmo
 * jeito que a tela Configuracoes > Personalizacao > Cores faz: transmitindo
 * uma mensagem WM_SETTINGCHANGE (com "ImmersiveColorSet") pra todas as
 * janelas de topo. Isso e feito via um P/Invoke de user32.dll disparado por
 * um script PowerShell (mesmo padrao do reg.exe: sem dependencia nativa
 * Java/JNA, so chamando uma ferramenta que ja vem no Windows).
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
        broadcastThemeChange();
        Logger.log("Tema do Windows alterado para " + theme);
    }

    /**
     * Avisa o Windows (Explorer, barra de tarefas, apps abertos) que o tema
     * mudou, transmitindo WM_SETTINGCHANGE / "ImmersiveColorSet" - o mesmo
     * broadcast que a tela de Configuracoes do Windows dispara. Sem isso o
     * registro muda mas a barra de tarefas so atualiza depois de algo mais
     * (reiniciar o Explorer, por exemplo).
     */
    private void broadcastThemeChange() {
        // Escrever o script num .ps1 temporario (em vez de passar via -Command)
        // evita problemas de quoting do Windows ao repassar aspas duplas do C#
        // pela linha de comando do ProcessBuilder.
        String script = """
                Add-Type -Namespace NightOut -Name Native -MemberDefinition '
                    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
                    public static extern IntPtr SendMessageTimeout(IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam, uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);
                ';
                $HWND_BROADCAST = [IntPtr]0xffff
                $WM_SETTINGCHANGE = 0x1A
                $SMTO_ABORTIFHUNG = 0x2
                $result = [UIntPtr]::Zero
                [NightOut.Native]::SendMessageTimeout($HWND_BROADCAST, $WM_SETTINGCHANGE, [UIntPtr]::Zero, 'ImmersiveColorSet', $SMTO_ABORTIFHUNG, 5000, [ref]$result) | Out-Null
                """;
        Path scriptFile;
        try {
            scriptFile = Files.createTempFile("nightout-theme-broadcast", ".ps1");
            Files.writeString(scriptFile, script, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.log("Aviso: falha ao preparar script de notificacao de tema: " + e);
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readAll(process);
            int exit = process.waitFor();
            if (exit != 0) {
                Logger.log("Aviso: broadcast de mudanca de tema retornou exit " + exit + ": " + output.trim());
            }
        } catch (IOException | InterruptedException e) {
            Logger.log("Aviso: falha ao notificar Windows sobre mudanca de tema: " + e);
        } finally {
            try {
                Files.deleteIfExists(scriptFile);
            } catch (IOException ignored) {
                // best effort
            }
        }
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
