package com.nightout;

import java.time.ZonedDateTime;

import com.nightout.WindowsThemeManager.Theme;

/**
 * Resultado de uma checagem do ThemeScheduler: o tema aplicado e os proximos
 * nascer/por do sol. No modo manual o sol nao e consultado, entao os horarios
 * ficam {@code null}.
 */
public record CheckResult(Theme theme, ZonedDateTime nextSunrise, ZonedDateTime nextSunset) {

    public boolean isManual() {
        return nextSunrise == null || nextSunset == null;
    }
}
