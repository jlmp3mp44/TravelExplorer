package com.travel.explorer.payload;

import com.travel.explorer.payload.place.PlaceResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityResponse {

  private Long id;

  private List<PlaceResponse> places = new ArrayList<>();
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  /** Average star rating (1–5), or null if there are no ratings yet. */
  private Double averageRating;

  private long ratingCount;
}
