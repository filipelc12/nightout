package com.nightout;

import com.nightout.WindowsThemeManager.Theme;

/** Como o tema e escolhido: automatico pelo sol ou fixo (manual) em claro/escuro. */
public enum ThemeMode {
    AUTO,
    LIGHT,
    DARK;

    /** Tema fixo do modo manual; {@code null} quando o modo e automatico. */
    public Theme manualTheme() {
        return switch (this) {
            case LIGHT -> Theme.LIGHT;
            case DARK -> Theme.DARK;
            case AUTO -> null;
        };
    }

    /** Le o valor salvo no config; qualquer coisa invalida volta para AUTO. */
    public static ThemeMode parse(String value) {
        if (value == null) return AUTO;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
