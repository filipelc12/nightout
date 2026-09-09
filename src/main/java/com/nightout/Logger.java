package com.nightout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Log simples: imprime no console e acumula em %APPDATA%\NightOut\nightout.log. */
public final class Logger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_DIR = Path.of(
            System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")), "NightOut");
    private static final Path LOG_FILE = LOG_DIR.resolve("nightout.log");

    private Logger() {
    }

    public static synchronized void log(String message) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        System.out.println(line);
        try {
            Files.createDirectories(LOG_DIR);
            Files.writeString(LOG_FILE, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Best effort - nao interrompe a aplicacao por falha de log.
        }
    }
}
