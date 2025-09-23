package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import java.util.List;

public interface PlaceService {

  List<Place> getAllRlaces();

  Place savePlace(Place place);

  Place deletePlace(Long placeId);

  Place updatePlace(Long placeId, Place place);
}
