package com.travel.explorer.payload.place;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceCategoryItemResponse {

  /** Google Places type string, e.g. {@code museum} */
  private String code;
  /** English label for UI */
  private String label;
}
