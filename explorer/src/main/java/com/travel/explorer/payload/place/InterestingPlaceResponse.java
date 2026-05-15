package com.travel.explorer.payload.place;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestingPlaceResponse {
  private Long id;
  private PlaceResponse place;
  private Long cityId;
  private String cityName;
  private Long countryId;
  private String countryName;
  private Instant createdAt;
}
