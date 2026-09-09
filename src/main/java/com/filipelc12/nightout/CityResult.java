package com.filipelc12.nightout;

/** Resultado de busca de cidade retornado pelo servico de geocoding. */
public record CityResult(String name, String admin1, String country, double lat, double lon) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (admin1 != null && !admin1.isBlank()) sb.append(", ").append(admin1);
        if (country != null && !country.isBlank()) sb.append(", ").append(country);
        return sb.toString();
    }
}
