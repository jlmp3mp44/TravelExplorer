package com.travel.explorer.payload.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GooglePlaceDto {

  /** Google Places short id (JSON field {@code id}). */
  @JsonProperty("id")
  private String googlePlaceId;
  private String formattedAddress;
  private DisplayNameDto displayName;
  private List<String> types;
}
