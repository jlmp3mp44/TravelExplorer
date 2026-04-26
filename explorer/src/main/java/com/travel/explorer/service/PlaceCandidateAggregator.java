package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.google.GooglePlaceService;
import com.travel.explorer.repo.PlaceRepo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PlaceCandidateAggregator {

  private final GooglePlaceService googlePlaceService;
  private final PlaceRepo placeRepo;

  public PlaceCandidateAggregator(GooglePlaceService googlePlaceService, PlaceRepo placeRepo) {
    this.googlePlaceService = googlePlaceService;
    this.placeRepo = placeRepo;
  }

  /**
   * Merges places from the Google Places API with stored places from the database,
   * deduplicating by googlePlaceId (primary) or title|address (fallback).
   * Google API results are preferred when duplicates are found.
   *
   * @param lat           center latitude
   * @param lng           center longitude
   * @param radius        search radius in meters
   * @param categoryTypes category / Google type names used for both API and DB queries
   * @return unified, deduplicated list of candidate places
   */
  public List<Place> aggregateCandidates(
      double lat, double lng, double radius, List<String> categoryTypes) {

    List<Place> googlePlaces = googlePlaceService.searchNearby(lat, lng, radius, categoryTypes);

    double deltaLat = radius / 111_000.0;
    double deltaLng = radius / (111_000.0 * Math.cos(Math.toRadians(lat)));

    List<Place> dbPlaces = placeRepo.findByCategoryNamesAndLocationBounds(
        categoryTypes,
        lat - deltaLat, lat + deltaLat,
        lng - deltaLng, lng + deltaLng);

    return merge(googlePlaces, dbPlaces);
  }

  private List<Place> merge(List<Place> googlePlaces, List<Place> dbPlaces) {
    Map<String, Place> seen = new LinkedHashMap<>();

    // Google results first — they take precedence over DB duplicates
    for (Place p : googlePlaces) {
      seen.put(dedupeKey(p), p);
    }

    for (Place p : dbPlaces) {
      seen.putIfAbsent(dedupeKey(p), p);
    }

    return new ArrayList<>(seen.values());
  }

  private static String dedupeKey(Place p) {
    if (p.getGooglePlaceId() != null && !p.getGooglePlaceId().isBlank()) {
      return p.getGooglePlaceId();
    }
    String title = p.getTitle() != null ? p.getTitle() : "";
    String address = p.getAddress() != null ? p.getAddress() : "";
    return title + "|" + address;
  }
}
