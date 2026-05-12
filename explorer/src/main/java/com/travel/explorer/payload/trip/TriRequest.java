package com.travel.explorer.payload.trip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.travel.explorer.entities.TripIntensity;
import com.travel.explorer.validation.ValidPlaceInterestCodes;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriRequest {
  @NotNull
  @JsonFormat(pattern="yyyy-MM-dd")
  private LocalDate startDate;

  @NotNull
  @JsonFormat(pattern="yyyy-MM-dd")
  private LocalDate endDate;
  private String country;
  private String city;
  private List<Long> cityIds;
  @NotNull
  private Integer budget;

  /**
   * Google Places type codes (see {@code GET /api/public/place-categories}).
   * Used to filter {@code places:searchNearby} via {@code includedTypes}.
   */
  @NotNull
  @NotEmpty
  @ValidPlaceInterestCodes
  @JsonAlias("interests")
  private List<String> categories;

  /** Defaults to true when omitted. */
  private Boolean isPublic;

  /** LOW / MEDIUM / HIGH — stored only; not used for logic yet. */
  private TripIntensity intensity;

  /**
   * Optional list of place ids the user explicitly wants in this trip (typically picked from
   * their "interesting places" matches). These bypass category filtering and receive a large
   * score boost during itinerary generation.
   */
  private List<Long> mustIncludePlaceIds;
}

