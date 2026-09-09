package com.nightout;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.nightout.WindowsThemeManager.Theme;

/**
 * Verifica periodicamente se o horario atual esta antes do nascer do sol, entre
 * nascer e por do sol, ou depois do por do sol, e ajusta o tema do Windows:
 *   - agora >= por do sol       -> tema escuro
 *   - nascer do sol <= agora    -> tema claro (dia)
 *   - antes do nascer do sol    -> tema escuro (ainda de noite)
 */
public final class ThemeScheduler {

    private final AppConfig config;
    private final SunTimesService sunTimesService = new SunTimesService();
    private final WindowsThemeManager themeManager = new WindowsThemeManager();
    private final Consumer<Theme> onThemeApplied;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nightout-scheduler");
        t.setDaemon(true);
        return t;
    });

    private SunTimes cachedSunTimes;
    private LocalDate cachedDate;

    public ThemeScheduler(AppConfig config, Consumer<Theme> onThemeApplied) {
        this.config = config;
        this.onThemeApplied = onThemeApplied;
    }

    public void start() {
        long intervalMinutes = Math.max(1, config.intervalMinutes);
        executor.scheduleAtFixedRate(this::safeCheck, 0, intervalMinutes, TimeUnit.MINUTES);
    }

    public void checkNow() {
        executor.submit(this::safeCheck);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void safeCheck() {
        try {
            check();
        } catch (Exception e) {
            Logger.log("Erro na checagem de tema: " + e);
        }
    }

    private void check() throws Exception {
        if (!config.isCityConfigured()) {
            Logger.log("Nenhuma cidade configurada ainda; pulando checagem.");
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        LocalDate today = now.toLocalDate();
        if (cachedSunTimes == null || !today.equals(cachedDate)) {
            cachedSunTimes = sunTimesService.fetchToday(config.lat, config.lon);
            cachedDate = today;
            Logger.log(String.format("Horarios de hoje (%s): nascer do sol %s, por do sol %s",
                    config.displayName(), cachedSunTimes.sunrise(), cachedSunTimes.sunset()));
        }

        boolean isDaytime = now.isAfter(cachedSunTimes.sunrise()) && now.isBefore(cachedSunTimes.sunset());
        Theme desired = isDaytime ? Theme.LIGHT : Theme.DARK;

        Theme current = themeManager.getCurrentTheme();
        if (current != desired) {
            themeManager.setTheme(desired);
        }
        onThemeApplied.accept(desired);
    }
}
