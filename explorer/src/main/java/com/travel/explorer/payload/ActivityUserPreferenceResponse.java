package com.travel.explorer.payload;

import com.travel.explorer.entities.ActivityChangeReason;
import com.travel.explorer.payload.place.PlaceResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-user customization for an activity (public trips: each traveler can differ). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityUserPreferenceResponse {

  private ActivityChangeReason reason;

  /** Place(s) this user prefers instead of the default activity places. */
  private List<PlaceResponse> replacementPlaces = new ArrayList<>();
}
