package com.travel.explorer.payload.place;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GooglePlaceDto {

  private String formattedAddress;
  private DisplayNameDto displayName;
  private List<String> types;
}
