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
 *   - agora >= por do sol de hoje    -> tema escuro
 *   - nascer do sol <= agora < por   -> tema claro (dia)
 *   - antes do nascer do sol de hoje -> tema escuro (ainda de noite)
 *
 * Para log/tooltip, tambem calcula o PROXIMO nascer e por do sol relevantes
 * (olhando pra frente, nunca um horario que ja passou):
 *   - antes do nascer de hoje  -> nascer de hoje, por do sol de hoje
 *   - depois do nascer de hoje, ainda de dia -> por do sol de hoje, nascer de amanha
 *   - depois do por do sol de hoje (de noite) -> nascer de amanha, por do sol de amanha
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

    private SunTimes todaySunTimes;
    private LocalDate todayCachedDate;

    private SunTimes tomorrowSunTimes;
    private LocalDate tomorrowCachedDate;

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
        ensureFetched(today);

        boolean isDaytime = now.isAfter(todaySunTimes.sunrise()) && now.isBefore(todaySunTimes.sunset());
        Theme desired = isDaytime ? Theme.LIGHT : Theme.DARK;

        ZonedDateTime nextSunrise;
        ZonedDateTime nextSunset;
        if (now.isBefore(todaySunTimes.sunrise())) {
            // Ainda nao amanheceu hoje: os dois proximos eventos sao hoje.
            nextSunrise = todaySunTimes.sunrise();
            nextSunset = todaySunTimes.sunset();
        } else if (now.isBefore(todaySunTimes.sunset())) {
            // Ja amanheceu, ainda nao anoiteceu: proximo por do sol e hoje,
            // proximo nascer do sol so amanha.
            ensureTomorrowFetched(today.plusDays(1));
            nextSunset = todaySunTimes.sunset();
            nextSunrise = tomorrowSunTimes != null ? tomorrowSunTimes.sunrise() : todaySunTimes.sunrise();
        } else {
            // Ja anoiteceu hoje: os dois proximos eventos sao amanha.
            ensureTomorrowFetched(today.plusDays(1));
            nextSunrise = tomorrowSunTimes != null ? tomorrowSunTimes.sunrise() : todaySunTimes.sunrise();
            nextSunset = tomorrowSunTimes != null ? tomorrowSunTimes.sunset() : todaySunTimes.sunset();
        }

        Logger.log(String.format("%s - proximo nascer do sol: %s, proximo por do sol: %s (tema: %s)",
                config.displayName(), nextSunrise, nextSunset, desired));

        Theme current = themeManager.getCurrentTheme();
        if (current != desired) {
            themeManager.setTheme(desired);
        }
        onThemeApplied.accept(desired);
    }

    private void ensureFetched(LocalDate today) throws Exception {
        if (todaySunTimes == null || !today.equals(todayCachedDate)) {
            todaySunTimes = sunTimesService.fetch(config.lat, config.lon, today);
            todayCachedDate = today;
            // O cache de amanha so vale enquanto "hoje" nao mudar.
            tomorrowSunTimes = null;
            tomorrowCachedDate = null;
        }
    }

    private void ensureTomorrowFetched(LocalDate tomorrow) {
        if (tomorrowSunTimes != null && tomorrow.equals(tomorrowCachedDate)) {
            return;
        }
        try {
            tomorrowSunTimes = sunTimesService.fetch(config.lat, config.lon, tomorrow);
            tomorrowCachedDate = tomorrow;
        } catch (Exception e) {
            Logger.log("Falha ao buscar nascer/por do sol de amanha: " + e);
            tomorrowSunTimes = null;
            tomorrowCachedDate = null;
        }
    }
}
