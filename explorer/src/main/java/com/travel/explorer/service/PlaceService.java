package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.PlaceListResponse;
import com.travel.explorer.payload.place.PlaceResponse;
import java.util.List;

public interface PlaceService {

  PlaceListResponse getAllPlaces(String sortOrder, String sortBy, Integer pageNumber, Integer pageSize);

  PlaceResponse savePlace(Place place);

  PlaceResponse deletePlace(Long placeId);

  PlaceResponse updatePlace(Long placeId, Place place);
}
