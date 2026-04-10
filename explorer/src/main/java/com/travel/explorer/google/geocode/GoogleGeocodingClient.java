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
    var uri = UriComponentsBuilder.fromUriString(BASE_URL)
        .queryParam("address", address)
        .queryParam("key", apiKey)
        .build()
        .encode()
        .toUri();
    return restTemplate.getForObject(uri, GeocodeResponse.class);
  }
}
