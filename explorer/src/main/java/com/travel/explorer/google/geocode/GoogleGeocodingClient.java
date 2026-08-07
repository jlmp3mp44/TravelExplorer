package com.travel.explorer.google.geocode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleGeocodingClient {

  private static final String BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

  private final String apiKey;
  private final RestTemplate restTemplate;

  public GoogleGeocodingClient(
      @Value("${google.api.key}") String apiKey,
      RestTemplateBuilder builder) {
    this.apiKey = apiKey;
    this.restTemplate = builder.build();
  }

  public GeocodeResponse geocode(String address) {
    return geocode(address, null);
  }

  /**
   * @param countryIso optional ISO 3166-1 alpha-2 (e.g. {@code IT}) to disambiguate city names
   */
  public GeocodeResponse geocode(String address, String countryIso) {
    var builder =
        UriComponentsBuilder.fromUriString(BASE_URL)
            .queryParam("address", address)
            .queryParam("key", apiKey);
    if (countryIso != null && !countryIso.isBlank()) {
      builder.queryParam("components", "country:" + countryIso.trim().toUpperCase());
    }
    var uri = builder.build().encode().toUri();
    return restTemplate.getForObject(uri, GeocodeResponse.class);
  }
}
