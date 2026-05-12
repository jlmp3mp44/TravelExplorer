package com.travel.explorer.service;

import com.travel.explorer.entities.City;
import com.travel.explorer.entities.Country;
import com.travel.explorer.entities.Place;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.google.GooglePlaceService;
import com.travel.explorer.google.geocode.GoogleGeocodingService;
import com.travel.explorer.google.geocode.LatLng;
import com.travel.explorer.payload.place.FreeTextPlaceSearchRequest;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.repo.CityRepository;
import com.travel.explorer.repo.CountryRepository;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Free-text Google Places search scoped to a city or country. Reused by the "Interesting
 * places" page so the user can search outside of any specific trip.
 */
@Service
public class PlaceSearchService {

  /** Wider than per-trip search since users may save places outside the standard 10km radius. */
  private static final int CITY_RADIUS_METERS = 30_000;
  private static final int COUNTRY_RADIUS_METERS = 50_000;

  @Autowired private GooglePlaceService googlePlaceService;
  @Autowired private GoogleGeocodingService googleGeocodingService;
  @Autowired private CityRepository cityRepository;
  @Autowired private CountryRepository countryRepository;
  @Autowired private ModelMapper modelMapper;

  public List<PlaceResponse> searchByFreeText(FreeTextPlaceSearchRequest request) {
    if (request == null
        || request.getQuery() == null
        || request.getQuery().isBlank()) {
      throw new APIException("query is required");
    }
    if (request.getCityId() == null && request.getCountryId() == null) {
      throw new APIException("Either cityId or countryId is required");
    }

    String address;
    int radius;
    if (request.getCityId() != null) {
      City city =
          cityRepository
              .findById(request.getCityId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("City", "cityId", request.getCityId()));
      address = buildAddressFromCity(city);
      radius = CITY_RADIUS_METERS;
    } else {
      Country country =
          countryRepository
              .findById(request.getCountryId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Country", "countryId", request.getCountryId()));
      address = country.getName() != null ? country.getName().trim() : "";
      radius = COUNTRY_RADIUS_METERS;
    }
    if (address.isBlank()) {
      throw new APIException("Could not resolve search location");
    }

    LatLng center = googleGeocodingService.geocodeToLatLng(address);
    List<Place> found =
        googlePlaceService.searchByFreeText(
            request.getQuery().trim(), center.latitude(), center.longitude(), radius);
    return found.stream().map(p -> modelMapper.map(p, PlaceResponse.class)).toList();
  }

  private static String buildAddressFromCity(City city) {
    String cityPart = city.getName() != null ? city.getName().trim() : "";
    String countryPart =
        city.getCountry() != null && city.getCountry().getName() != null
            ? city.getCountry().getName().trim()
            : "";
    if (!cityPart.isEmpty() && !countryPart.isEmpty()) {
      return cityPart + ", " + countryPart;
    }
    if (!cityPart.isEmpty()) {
      return cityPart;
    }
    return countryPart;
  }
}
