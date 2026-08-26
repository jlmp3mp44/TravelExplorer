package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.google.GooglePlaceClient;
import com.travel.explorer.google.GooglePlacePhotoMediaUrlBuilder;
import com.travel.explorer.payload.place.GooglePlaceDto;
import com.travel.explorer.repo.PlaceRepo;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class PlacePhotoService {

  private static final Logger log = LoggerFactory.getLogger(PlacePhotoService.class);

  public static final String PUBLIC_PHOTO_PATH_PREFIX = "/api/public/places/";

  private final PlaceRepo placeRepo;
  private final GooglePlaceClient googlePlaceClient;
  private final GooglePlacePhotoMediaUrlBuilder photoMediaUrlBuilder;
  private final RestTemplate restTemplate;

  public PlacePhotoService(
      PlaceRepo placeRepo,
      GooglePlaceClient googlePlaceClient,
      GooglePlacePhotoMediaUrlBuilder photoMediaUrlBuilder,
      RestTemplateBuilder restTemplateBuilder) {
    this.placeRepo = placeRepo;
    this.googlePlaceClient = googlePlaceClient;
    this.photoMediaUrlBuilder = photoMediaUrlBuilder;
    this.restTemplate = restTemplateBuilder.build();
  }

  public static String publicPhotoUrl(Long placeId) {
    if (placeId == null) {
      return null;
    }
    return PUBLIC_PHOTO_PATH_PREFIX + placeId + "/photo";
  }

  @Transactional
  public Optional<PlacePhotoPayload> loadPhoto(Long placeId) {
    Place place =
        placeRepo
            .findById(placeId)
            .orElseThrow(() -> new ResourceNotFoundException("Place", "placeId", placeId));

    if (place.getGooglePlaceId() == null || place.getGooglePlaceId().isBlank()) {
      return loadFromStoredUrl(place);
    }

    try {
      GooglePlaceDto dto = googlePlaceClient.getPlaceDetails(place.getGooglePlaceId());
      String mediaUrl = photoMediaUrlBuilder.firstPhotoMediaUrl(dto);
      if (mediaUrl == null || mediaUrl.isBlank()) {
        return loadFromStoredUrl(place);
      }
      place.setPhotoUrl(mediaUrl);
      placeRepo.save(place);
      return fetchBytes(mediaUrl, MediaType.IMAGE_JPEG);
    } catch (Exception e) {
      log.warn("Failed to refresh photo for place {}: {}", placeId, e.getMessage());
      return loadFromStoredUrl(place);
    }
  }

  private Optional<PlacePhotoPayload> loadFromStoredUrl(Place place) {
    String url = place.getPhotoUrl();
    if (url == null || url.isBlank()) {
      return Optional.empty();
    }
    return fetchBytes(url, MediaType.IMAGE_JPEG);
  }

  private Optional<PlacePhotoPayload> fetchBytes(String url, MediaType contentType) {
    try {
      byte[] body = restTemplate.getForObject(url, byte[].class);
      if (body == null || body.length == 0) {
        return Optional.empty();
      }
      return Optional.of(new PlacePhotoPayload(body, contentType));
    } catch (Exception e) {
      log.warn("Failed to download place photo from {}: {}", url, e.getMessage());
      return Optional.empty();
    }
  }

  public record PlacePhotoPayload(byte[] body, MediaType contentType) {
    public HttpHeaders cacheHeaders() {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(contentType != null ? contentType : MediaType.IMAGE_JPEG);
      headers.setCacheControl("public, max-age=86400");
      return headers;
    }
  }
}
