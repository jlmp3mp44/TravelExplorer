package com.travel.explorer.payload.place;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/public/places/search-text}. At least one of {@code cityId}
 * / {@code countryId} must be supplied so we can geocode a search center.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreeTextPlaceSearchRequest {
  @NotBlank
  private String query;

  private Long cityId;
  private Long countryId;
}
