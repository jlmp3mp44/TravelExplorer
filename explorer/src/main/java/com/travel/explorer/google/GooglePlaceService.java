package com.travel.explorer.google;

import com.travel.explorer.entities.Place;
import com.travel.explorer.google.request.Center;
import com.travel.explorer.google.request.Circle;
import com.travel.explorer.google.request.LocationRestriction;
import com.travel.explorer.google.request.SearchNearbyRequest;
import com.travel.explorer.google.response.GooglePlacesResponse;
import com.travel.explorer.payload.place.GooglePlaceDto;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GooglePlaceService {

  private final GooglePlaceClient client;
  private final GooglePlaceMapper mapper;

  // Інжектимо і клієнт, і мапер
  public GooglePlaceService(GooglePlaceClient client, GooglePlaceMapper mapper) {
    this.client = client;
    this.mapper = mapper;
  }

  // Тепер метод повертає List<Place> (ваші внутрішні Entity/Моделі)
  public List<Place> searchNearby(double latitude, double longitude, double radius) {
    SearchNearbyRequest request = new SearchNearbyRequest(
        new LocationRestriction(
            new Circle(new Center(latitude, longitude), radius)
        )
    );

    GooglePlacesResponse response = client.searchNearby(request);

    if (response == null || response.getPlaces() == null) {
      return Collections.emptyList();
    }

    // Мапимо DTO від Google у ваші сутності одразу тут
    return response.getPlaces().stream()
        .map(mapper::toPlace)
        .collect(Collectors.toList());
  }
}