package com.travel.explorer.service.scheduling;

import com.travel.explorer.entities.Place;
import java.util.ArrayList;
import java.util.List;

/** Filters places by distance from a trip search center. */
public final class PlaceGeoFilter {

  private PlaceGeoFilter() {}

  /**
   * Keeps only places with a location within {@code maxRadiusMeters} of the center (Haversine).
   */
  public static List<Place> withinRadius(
      List<Place> places, double centerLat, double centerLng, double maxRadiusMeters) {
    if (places == null || places.isEmpty()) {
      return List.of();
    }
    double maxKm = maxRadiusMeters / 1000.0;
    List<Place> out = new ArrayList<>(places.size());
    for (Place place : places) {
      if (place.getLocation() == null) {
        continue;
      }
      double km =
          HaversineUtil.distanceKm(
              centerLat,
              centerLng,
              place.getLocation().getLat(),
              place.getLocation().getLng());
      if (km <= maxKm) {
        out.add(place);
      }
    }
    return out;
  }
}
