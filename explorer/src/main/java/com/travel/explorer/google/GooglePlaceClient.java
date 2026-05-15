package com.travel.explorer.google;

import com.travel.explorer.google.request.SearchNearbyRequest;
import com.travel.explorer.google.request.TextSearchRequest;
import com.travel.explorer.google.response.GooglePlacesResponse;
import com.travel.explorer.payload.place.GooglePlaceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GooglePlaceClient {

  private final String apiKey;
  private final RestTemplate restTemplate;

  private static final String TEXT_SEARCH_URL =
      "https://places.googleapis.com/v1/places:searchText";

  private static final String PLACE_DETAILS_URL =
      "https://places.googleapis.com/v1/places/";

  /** Cheapest SKU ($0.30/1K) — only retrieves place IDs. */
  private static final String ID_ONLY_FIELD_MASK = "places.id,nextPageToken";

  /** Full detail field mask for Text Search or Place Details. */
  private static final String FULL_FIELD_MASK =
      "places.id,places.displayName,places.formattedAddress,places.types,places.location,"
          + "places.primaryType,places.rating,places.userRatingCount,places.businessStatus,"
          + "places.priceLevel,places.photos,nextPageToken";

  /** Field mask for single Place Details (GET) — no "places." prefix. */
  private static final String DETAIL_FIELD_MASK =
      "id,displayName,formattedAddress,types,location,"
          + "primaryType,rating,userRatingCount,businessStatus,priceLevel,photos";

  public GooglePlaceClient(@Value("${google.api.key}") String apiKey, RestTemplateBuilder builder) {
    this.apiKey = apiKey;
    this.restTemplate = builder.build();
  }

  /**
   * Text Search with ID-only field mask (cheapest SKU).
   * Returns place IDs + nextPageToken for pagination.
   */
  public GooglePlacesResponse searchTextIds(TextSearchRequest requestBody) {
    return postTextSearch(requestBody, ID_ONLY_FIELD_MASK);
  }

  /**
   * Text Search with full detail field mask.
   */
  public GooglePlacesResponse searchTextFull(TextSearchRequest requestBody) {
    return postTextSearch(requestBody, FULL_FIELD_MASK);
  }

  /**
   * Fetches full details for a single place by its Google Place ID.
   * Uses GET {@code /v1/places/{placeId}} endpoint.
   */
  public GooglePlaceDto getPlaceDetails(String placeId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set("X-Goog-FieldMask", DETAIL_FIELD_MASK);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<GooglePlaceDto> response = restTemplate.exchange(
        PLACE_DETAILS_URL + placeId,
        HttpMethod.GET,
        entity,
        GooglePlaceDto.class
    );
    return response.getBody();
  }

  /** @deprecated Use {@link #searchTextIds} or {@link #searchTextFull} instead. */
  @Deprecated
  public GooglePlacesResponse searchNearby(SearchNearbyRequest requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set(
        "X-Goog-FieldMask",
        "places.id,places.displayName,places.formattedAddress,places.types,places.location,"
            + "places.primaryType,places.rating,places.userRatingCount,places.businessStatus,"
            + "places.priceLevel,places.photos");

    HttpEntity<SearchNearbyRequest> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<GooglePlacesResponse> response = restTemplate.exchange(
        "https://places.googleapis.com/v1/places:searchNearby",
        HttpMethod.POST,
        entity,
        GooglePlacesResponse.class
    );
    return response.getBody();
  }

  private GooglePlacesResponse postTextSearch(TextSearchRequest requestBody, String fieldMask) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set("X-Goog-FieldMask", fieldMask);

    HttpEntity<TextSearchRequest> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<GooglePlacesResponse> response = restTemplate.exchange(
        TEXT_SEARCH_URL,
        HttpMethod.POST,
        entity,
        GooglePlacesResponse.class
    );
    return response.getBody();
  }
}
