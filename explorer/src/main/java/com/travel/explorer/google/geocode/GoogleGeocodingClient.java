package com.travel.explorer.google.geocode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleGeocodingClient {

  private final String apiKey;
  private final String baseUrl;
  private final RestTemplate restTemplate;

  public GoogleGeocodingClient(
      @Value("${google.api.key}") String apiKey,
      @Value("${google.geocoding.base-url}") String baseUrl,
      RestTemplateBuilder builder) {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl.replaceAll("/+$", "");
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
        UriComponentsBuilder.fromUriString(baseUrl + "/maps/api/geocode/json")
            .queryParam("address", address)
            .queryParam("key", apiKey);
    if (countryIso != null && !countryIso.isBlank()) {
      builder.queryParam("components", "country:" + countryIso.trim().toUpperCase());
    }
    var uri = builder.build().encode().toUri();
    return restTemplate.getForObject(uri, GeocodeResponse.class);
  }
}
