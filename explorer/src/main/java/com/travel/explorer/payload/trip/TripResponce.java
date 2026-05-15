package com.travel.explorer.payload.trip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travel.explorer.entities.TripIntensity;
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

  /** Pace / density from trip creation, if set. */
  private TripIntensity intensity;

  private List<DayResponse> days;

  private Boolean isPublic;

  /** Present when the trip was created by a logged-in user; use to detect ownership on the client. */
  private Long ownerId;

  /**
   * Owner profile (id, username, email, phone). Serialized as {@code "owner"} so ModelMapper does
   * not match {@link com.travel.explorer.entities.Trip#getOwner()} to this field by name.
   */
  @JsonProperty("owner")
  private TripOwnerResponse ownerProfile;

  /** Average star rating (1–5), or null if there are no ratings yet. */
  private Double averageRating;

  private long ratingCount;

  private EstimatedBudget estimatedBudget;

  /** Cover image: first place photo in itinerary order (by day date, activity order, place order). */
  private String coverPhotoUrl;
}

