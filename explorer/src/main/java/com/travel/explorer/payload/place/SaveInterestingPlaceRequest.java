package com.travel.explorer.payload.place;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveInterestingPlaceRequest {
  @NotNull
  private Long placeId;

  /** Optional context: the city the user was searching in when they saved the place. */
  private Long cityId;

  /** Optional context: the country the user was searching in when they saved the place. */
  private Long countryId;
}
