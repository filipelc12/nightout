package com.filipelc12.nightout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuracao persistida do usuario: cidade escolhida e intervalo de checagem.
 * Guardada em %APPDATA%\NightOut\config.properties.
 */
public final class AppConfig {

    private static final Path CONFIG_DIR = Path.of(
            System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")), "NightOut");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    public String cityName;
    public String admin1;
    public String country;
    public Double lat;
    public Double lon;
    public int intervalMinutes = 5;

    public boolean isCityConfigured() {
        return lat != null && lon != null;
    }

    public static AppConfig load() {
        AppConfig config = new AppConfig();
        if (!Files.exists(CONFIG_FILE)) {
            return config;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            Logger.log("Falha ao ler config: " + e.getMessage());
            return config;
        }
        config.cityName = props.getProperty("city.name");
        config.admin1 = props.getProperty("city.admin1");
        config.country = props.getProperty("city.country");
        String latStr = props.getProperty("city.lat");
        String lonStr = props.getProperty("city.lon");
        if (latStr != null) config.lat = Double.valueOf(latStr);
        if (lonStr != null) config.lon = Double.valueOf(lonStr);
        config.intervalMinutes = Integer.parseInt(props.getProperty("check.interval.minutes", "5"));
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            Logger.log("Falha ao criar diretorio de config: " + e.getMessage());
            return;
        }
        Properties props = new Properties();
        if (cityName != null) props.setProperty("city.name", cityName);
        if (admin1 != null) props.setProperty("city.admin1", admin1);
        if (country != null) props.setProperty("city.country", country);
        if (lat != null) props.setProperty("city.lat", String.valueOf(lat));
        if (lon != null) props.setProperty("city.lon", String.valueOf(lon));
        props.setProperty("check.interval.minutes", String.valueOf(intervalMinutes));
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "Configuracao NightOut");
        } catch (IOException e) {
            Logger.log("Falha ao salvar config: " + e.getMessage());
        }
    }

    public String displayName() {
        if (cityName == null) return "(nenhuma cidade selecionada)";
        StringBuilder sb = new StringBuilder(cityName);
        if (admin1 != null && !admin1.isBlank()) sb.append(", ").append(admin1);
        if (country != null && !country.isBlank()) sb.append(", ").append(country);
        return sb.toString();
    }
}
