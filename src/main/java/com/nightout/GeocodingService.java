package com.nightout;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Busca cidades por nome usando a API publica e gratuita de geocoding do Open-Meteo
 * (nao requer chave de API). https://open-meteo.com/en/docs/geocoding-api
 */
public final class GeocodingService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<CityResult> search(String query) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create("https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=10&language=pt&format=json");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Geocoding API retornou status " + response.statusCode());
        }
        return parseResults(response.body());
    }

    @SuppressWarnings("unchecked")
    private List<CityResult> parseResults(String body) {
        Map<String, Object> root = Json.parseObject(body);
        Object resultsObj = root.get("results");
        List<CityResult> cities = new ArrayList<>();
        if (!(resultsObj instanceof List<?> results)) {
            return cities;
        }
        for (Object item : results) {
            Map<String, Object> entry = (Map<String, Object>) item;
            String name = (String) entry.get("name");
            String admin1 = (String) entry.get("admin1");
            String country = (String) entry.get("country");
            double lat = ((Number) entry.get("latitude")).doubleValue();
            double lon = ((Number) entry.get("longitude")).doubleValue();
            cities.add(new CityResult(name, admin1, country, lat, lon));
        }
        return cities;
    }
}
