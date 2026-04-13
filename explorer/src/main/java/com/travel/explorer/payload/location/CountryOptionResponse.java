package com.travel.explorer.payload.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryOptionResponse {

  private Long id;
  private String name;
  private String iso;
}
