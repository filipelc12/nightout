package com.nightout;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

/**
 * Consulta o horario de nascer e por do sol de uma data especifica para uma
 * coordenada, usando a API publica e gratuita sunrise-sunset.org.
 */
public final class SunTimesService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Busca o nascer/por do sol de hoje (data local do sistema). */
    public SunTimes fetchToday(double lat, double lon) throws IOException, InterruptedException {
        return fetch(lat, lon, LocalDate.now());
    }

    /** Busca o nascer/por do sol de uma data especifica (ex.: amanha). */
    public SunTimes fetch(double lat, double lon, LocalDate date) throws IOException, InterruptedException {
        URI uri = URI.create(String.format(Locale.ROOT,
                "https://api.sunrise-sunset.org/json?lat=%f&lng=%f&formatted=0&date=%s", lat, lon, date));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Sunrise-sunset API retornou status " + response.statusCode());
        }
        return parse(response.body());
    }

    @SuppressWarnings("unchecked")
    private SunTimes parse(String body) {
        Map<String, Object> root = Json.parseObject(body);
        String status = (String) root.get("status");
        if (!"OK".equals(status)) {
            throw new IllegalStateException("Sunrise-sunset API retornou status logico: " + status);
        }
        Map<String, Object> results = (Map<String, Object>) root.get("results");
        ZoneId systemZone = ZoneId.systemDefault();
        var sunrise = OffsetDateTime.parse((String) results.get("sunrise")).atZoneSameInstant(systemZone);
        var sunset = OffsetDateTime.parse((String) results.get("sunset")).atZoneSameInstant(systemZone);
        return new SunTimes(sunrise, sunset);
    }
}
