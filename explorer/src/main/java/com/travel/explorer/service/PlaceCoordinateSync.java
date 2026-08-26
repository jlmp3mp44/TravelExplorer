package com.travel.explorer.service;

import com.travel.explorer.entities.Location;
import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.GooglePlaceDto;

/** Copies coordinates from a fresh Google result onto a persisted {@link Place}. */
public final class PlaceCoordinateSync {

  private PlaceCoordinateSync() {}

  public static void applyFreshLocation(Place target, Place source) {
    if (target == null || source == null || source.getLocation() == null) {
      return;
    }
    Location src = source.getLocation();
    Location loc = target.getLocation();
    if (loc == null) {
      loc = new Location();
      target.setLocation(loc);
    }
    loc.setLat(src.getLat());
    loc.setLng(src.getLng());
  }

  public static void applyFreshLocation(Place target, GooglePlaceDto dto) {
    if (target == null || dto == null || dto.getLocation() == null) {
      return;
    }
    Location loc = target.getLocation();
    if (loc == null) {
      loc = new Location();
      target.setLocation(loc);
    }
    loc.setLat(dto.getLocation().latitude());
    loc.setLng(dto.getLocation().longitude());
  }
}
