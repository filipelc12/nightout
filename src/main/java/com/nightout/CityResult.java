package com.nightout;

/** Resultado de busca de cidade retornado pelo servico de geocoding. */
public record CityResult(String name, String region, String country, double lat, double lon) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (region != null && !region.isBlank()) sb.append(", ").append(region);
        if (country != null && !country.isBlank()) sb.append(", ").append(country);
        return sb.toString();
    }
}
