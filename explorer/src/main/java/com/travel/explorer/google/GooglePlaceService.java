package com.travel.explorer.google;

import com.travel.explorer.entities.Place;
import com.travel.explorer.google.request.Center;
import com.travel.explorer.google.request.Circle;
import com.travel.explorer.google.request.LocationRestriction;
import com.travel.explorer.google.request.SearchNearbyRequest;
import com.travel.explorer.google.response.GooglePlacesResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GooglePlaceService {

  private static final int MAX_TYPES_PER_REQUEST = 50;
  private static final String LANGUAGE_UK = "uk";

  private final GooglePlaceClient client;
  private final GooglePlaceMapper mapper;

  public GooglePlaceService(GooglePlaceClient client, GooglePlaceMapper mapper) {
    this.client = client;
    this.mapper = mapper;
  }

  /**
   * Nearby search restricted to the given Google Places types (includedTypes).
   * Chunks types beyond {@value #MAX_TYPES_PER_REQUEST} and merges results, deduping by place id.
   */
  public List<Place> searchNearby(
      double latitude, double longitude, double radius, List<String> includedTypes) {
    if (includedTypes == null || includedTypes.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> distinct = includedTypes.stream().distinct().toList();
    List<Place> merged = new ArrayList<>();
    Map<String, Place> byId = new LinkedHashMap<>();

    for (int i = 0; i < distinct.size(); i += MAX_TYPES_PER_REQUEST) {
      int end = Math.min(i + MAX_TYPES_PER_REQUEST, distinct.size());
      List<String> chunk = distinct.subList(i, end);
      SearchNearbyRequest request =
          new SearchNearbyRequest(
              new LocationRestriction(new Circle(new Center(latitude, longitude), radius)),
              20,
              chunk,
              LANGUAGE_UK);

      GooglePlacesResponse response = client.searchNearby(request);
      if (response == null || response.getPlaces() == null) {
        continue;
      }
      for (Place p : response.getPlaces().stream().map(mapper::toPlace).toList()) {
        String key = placeDedupeKey(p);
        if (!byId.containsKey(key)) {
          byId.put(key, p);
          merged.add(p);
        }
      }
    }
    return merged;
  }

  private static String placeDedupeKey(Place p) {
    if (p.getGooglePlaceId() != null && !p.getGooglePlaceId().isBlank()) {
      return p.getGooglePlaceId();
    }
    String title = p.getTitle() != null ? p.getTitle() : "";
    String addr = p.getAddress() != null ? p.getAddress() : "";
    return title + "|" + addr;
  }
}