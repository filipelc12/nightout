package com.nightout;

import java.time.ZonedDateTime;

import com.nightout.WindowsThemeManager.Theme;

/**
 * Resultado de uma checagem do ThemeScheduler: o tema aplicado e os proximos
 * nascer/por do sol. No modo manual o sol nao e consultado, entao os horarios
 * ficam {@code null}. Em modo automatico, um horario indisponivel tambem e null.
 */
public record CheckResult(Theme theme, ZonedDateTime nextSunrise, ZonedDateTime nextSunset, boolean manual) {

    public CheckResult(Theme theme, ZonedDateTime nextSunrise, ZonedDateTime nextSunset) {
        this(theme, nextSunrise, nextSunset, false);
    }

    public boolean isManual() {
        return manual;
    }
}
