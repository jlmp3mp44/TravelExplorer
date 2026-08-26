package com.travel.explorer.service.scheduling;

import com.travel.explorer.google.request.LatLngPoint;
import com.travel.explorer.google.request.Rectangle;

/** Builds geographic bounding boxes for Places API location restrictions. */
public final class GeoBounds {

  private GeoBounds() {}

  /**
   * Southwest / northeast corners for a circle around {@code centerLat}/{@code centerLng}.
   * Used as {@code locationRestriction.rectangle} (results must fall inside the box).
   */
  public static Rectangle boundingRectangle(
      double centerLat, double centerLng, double radiusMeters) {
    double deltaLat = radiusMeters / 111_000.0;
    double cosLat = Math.cos(Math.toRadians(centerLat));
    double deltaLng = radiusMeters / (111_000.0 * Math.max(cosLat, 0.01));
    double minLat = centerLat - deltaLat;
    double maxLat = centerLat + deltaLat;
    double minLng = centerLng - deltaLng;
    double maxLng = centerLng + deltaLng;
    return new Rectangle(
        new LatLngPoint(minLat, minLng), new LatLngPoint(maxLat, maxLng));
  }
}
