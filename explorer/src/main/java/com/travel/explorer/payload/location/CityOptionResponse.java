package com.travel.explorer.payload.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityOptionResponse {

  private Long id;
  private String name;
  private Long countryId;
}
