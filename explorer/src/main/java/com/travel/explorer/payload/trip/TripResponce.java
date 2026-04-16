package com.travel.explorer.payload.trip;

import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.DayResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripResponce {
  private Integer id;
  private String title;
  private String desc;
  private String startDate;
  private String endDate;
  /** Google Places type codes selected for this trip */
  private List<String> categories;
  private List<DayResponse> days;

  private Boolean isPublic;

  /** Average star rating (1–5), or null if there are no ratings yet. */
  private Double averageRating;

  private long ratingCount;
}

