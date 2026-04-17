package com.travel.explorer.payload.trip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.travel.explorer.validation.ValidPlaceInterestCodes;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial update for a trip. Null fields are left unchanged.
 * Set {@link #regenerateItinerary} to true to rebuild days/activities from current or updated
 * dates, cities, and categories (e.g. after changing city or interests).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripUpdateRequest {

  @Size(min = 3, max = 120)
  private String title;

  private String desc;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  private Integer budget;

  private Boolean isPublic;

  /** When set, replaces the trip's cities (same rules as creating a trip). */
  private List<Long> cityIds;

  @ValidPlaceInterestCodes
  @JsonAlias("interests")
  private List<String> categories;

  /**
   * When true, existing days and activities are removed and the itinerary is generated again
   * using the effective dates, cities, and categories (from this request and/or stored on the trip).
   */
  private Boolean regenerateItinerary;
}
