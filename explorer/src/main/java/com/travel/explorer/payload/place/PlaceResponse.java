package com.travel.explorer.payload.place;

import com.travel.explorer.entities.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {

  /** Persisted place id (use as {@code placeId} when replacing an activity). */
  private Long id;

  private String title;
  private Location location;
}
