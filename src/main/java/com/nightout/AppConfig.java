package com.nightout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuracao persistida do usuario: cidade escolhida, intervalo de checagem e modo do tema.
 * Guardada em %APPDATA%\NightOut\config.properties.
 */
public final class AppConfig {

    private static final Path CONFIG_DIR = Path.of(
            System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")), "NightOut");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    public String cityName;
    public String region;
    public String country;
    public Double lat;
    public Double lon;
    public int intervalMinutes = 5;
    public String wallpaperDayPath;
    public String wallpaperNightPath;
    public ThemeMode themeMode = ThemeMode.AUTO;

    /*
    A configuracao da cidade é basicamente latitude e longitude.
     */
    public boolean isCityConfigured() {
        return lat != null && lon != null;
    }

    public boolean isWallpaperConfigured() {
        return (wallpaperDayPath != null && !wallpaperDayPath.isBlank())
                || (wallpaperNightPath != null && !wallpaperNightPath.isBlank());
    }

    public static AppConfig load() {
        // Cria um config e caso nao exista retorna ele vazio
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
        }//TODO adicionar mais tipos de excecao em um futuro

        return fromProperties(props);
    }

    private static AppConfig fromProperties(Properties props) {
        AppConfig config = new AppConfig();
        config.cityName = props.getProperty("city.name");
        config.region = props.getProperty("city.region");
        config.country = props.getProperty("city.country");
        String latStr = props.getProperty("city.lat");
        String lonStr = props.getProperty("city.lon");
        if (latStr != null) config.lat = Double.valueOf(latStr);
        if (lonStr != null) config.lon = Double.valueOf(lonStr);
        config.intervalMinutes = Integer.parseInt(props.getProperty("check.interval.minutes", "5"));
        config.wallpaperDayPath = props.getProperty("wallpaper.day.path");
        config.wallpaperNightPath = props.getProperty("wallpaper.night.path");
        config.themeMode = ThemeMode.parse(props.getProperty("theme.mode"));
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
        if (region != null) props.setProperty("city.region", region);
        if (country != null) props.setProperty("city.country", country);
        if (lat != null) props.setProperty("city.lat", String.valueOf(lat));
        if (lon != null) props.setProperty("city.lon", String.valueOf(lon));
        props.setProperty("check.interval.minutes", String.valueOf(intervalMinutes));
        if (wallpaperDayPath != null) props.setProperty("wallpaper.day.path", wallpaperDayPath);
        if (wallpaperNightPath != null) props.setProperty("wallpaper.night.path", wallpaperNightPath);
        props.setProperty("theme.mode", themeMode.name());
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "Configuracao NightOut");
        } catch (IOException e) {
            Logger.log("Falha ao salvar config: " + e.getMessage());
        }
    }

    public String displayName() {
        if (cityName == null) return "(nenhuma cidade selecionada)";
        StringBuilder sb = new StringBuilder(cityName);
        if (region != null && !region.isBlank()) sb.append(", ").append(region);
        if (country != null && !country.isBlank()) sb.append(", ").append(country);
        return sb.toString();
    }
}
