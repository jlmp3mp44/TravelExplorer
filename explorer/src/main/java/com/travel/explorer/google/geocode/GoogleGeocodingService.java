package com.travel.explorer.google.geocode;

import com.travel.explorer.excpetions.APIException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class GoogleGeocodingService {

  private static final Logger log = LoggerFactory.getLogger(GoogleGeocodingService.class);

  private final GoogleGeocodingClient client;

  public GoogleGeocodingService(GoogleGeocodingClient client) {
    this.client = client;
  }

  public LatLng geocodeToLatLng(String address) {
    GeocodeResponse response;
    try {
      response = client.geocode(address);
    } catch (RestClientException e) {
      log.warn("Geocoding request failed for '{}'", address, e);
      throw new APIException("Could not resolve destination: " + e.getMessage());
    }
    if (response == null) {
      throw new APIException("Geocoding failed: empty response");
    }

    String status = response.status();
    if (!"OK".equals(status)) {
      String detail = response.errorMessage() != null ? response.errorMessage() : status;
      log.warn("Geocoding error for '{}': {}", address, detail);
      throw new APIException("Could not resolve destination: " + detail);
    }

    List<GeocodeResult> results = response.results();
    if (results == null || results.isEmpty()) {
      throw new APIException("Could not resolve destination: no results");
    }

    GeocodeResult first = results.get(0);
    if (first == null || first.geometry() == null || first.geometry().location() == null) {
      throw new APIException("Could not resolve destination: invalid result");
    }

    GeocodeLocation loc = first.geometry().location();
    return new LatLng(loc.lat(), loc.lng());
  }
}
