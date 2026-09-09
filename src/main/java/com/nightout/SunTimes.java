package com.nightout;

import java.time.ZonedDateTime;

/** Horarios de nascer e por do sol, ja convertidos para o fuso horario local do sistema. */
public record SunTimes(ZonedDateTime sunrise, ZonedDateTime sunset) {
}
