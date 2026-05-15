package com.travel.explorer.google;

import com.travel.explorer.payload.place.GooglePlaceDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds browser-usable Google Place Photo media URLs (first party API) for {@code <img src>}.
 */
@Component
public class GooglePlacePhotoMediaUrlBuilder {

  private final String apiKey;
  private final int maxHeightPx;

  public GooglePlacePhotoMediaUrlBuilder(
      @Value("${google.api.key}") String apiKey,
      @Value("${google.place-photo.max-height-px:800}") int maxHeightPx) {
    this.apiKey = apiKey;
    this.maxHeightPx = maxHeightPx;
  }

  /**
   * @param photoResourceName full resource name from the Places API, e.g. {@code
   *     places/ChIJ…/photos/AWn5…}
   */
  public String buildMediaUrl(String photoResourceName) {
    if (photoResourceName == null || photoResourceName.isBlank()) {
      return null;
    }
    return UriComponentsBuilder.fromUriString(
            "https://places.googleapis.com/v1/" + photoResourceName + "/media")
        .queryParam("maxHeightPx", maxHeightPx)
        .queryParam("key", apiKey)
        .build()
        .toUriString();
  }

  /** Uses the first photo returned by Google for this place, if any. */
  public String firstPhotoMediaUrl(GooglePlaceDto dto) {
    if (dto == null || dto.getPhotos() == null || dto.getPhotos().isEmpty()) {
      return null;
    }
    GooglePlaceDto.PhotoRef first = dto.getPhotos().get(0);
    if (first == null || first.getName() == null || first.getName().isBlank()) {
      return null;
    }
    return buildMediaUrl(first.getName());
  }
}
