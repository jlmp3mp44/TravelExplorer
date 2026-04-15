package com.travel.explorer.google;

import com.travel.explorer.google.request.SearchNearbyRequest;
import com.travel.explorer.google.response.GooglePlacesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GooglePlaceClient {

  private final String apiKey;
  private final RestTemplate restTemplate;
  private final String baseUrl = "https://places.googleapis.com/v1/places:searchNearby";

  public GooglePlaceClient(@Value("${google.api.key}") String apiKey, RestTemplateBuilder builder) {
    this.apiKey = apiKey;
    this.restTemplate = builder.build();
  }

  public GooglePlacesResponse searchNearby(SearchNearbyRequest requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set(
        "X-Goog-FieldMask",
        "places.id,places.displayName,places.formattedAddress,places.types,places.location");

    HttpEntity<SearchNearbyRequest> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<GooglePlacesResponse> response = restTemplate.exchange(
        baseUrl,
        HttpMethod.POST,
        entity,
        GooglePlacesResponse.class
    );

    return response.getBody();
  }
}