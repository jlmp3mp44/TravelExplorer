package com.travel.explorer.payload.trip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Manual append: only {@code placeId} (same place resolution as replace-with-place). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTripActivityRequest {

  private Long placeId;
}
