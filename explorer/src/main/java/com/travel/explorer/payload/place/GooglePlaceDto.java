package com.travel.explorer.payload.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travel.explorer.google.request.Center;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GooglePlaceDto {

  /** Google Places short id (JSON field {@code id}). */
  @JsonProperty("place_id")
  private String googlePlaceId;
  private String formattedAddress;
  private DisplayNameDto displayName;
  private List<String> types;
  /** Google Places LatLng ({@code latitude} / {@code longitude}). */
  private Center location;
  private String primaryType;
  private Double rating;
  private Integer userRatingCount;
  private String businessStatus;
}
