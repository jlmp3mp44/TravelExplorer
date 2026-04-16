package com.travel.explorer.service;

import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.trip.ReplaceActivityRequest;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import java.util.List;

public interface TripService {

  TripListResponce getAllTrips(String sortBy, String sortOrder, Integer pageNumber, Integer pageSize);

  TripResponce saveTrip(TriRequest triRequest);

  TripResponce deleteTrip(Long tripId);

  TripResponce updateTrip(Long tripId, Trip trip);

  /**
   * @param userId optional; when set, each activity may include {@code userPreference} for that user's
   *     overrides (shared public trips).
   */
  TripResponce getTripById(Long tripId, Long userId);

  /** Sets {@code sortOrder} on each activity so it matches the given order (same day, full permutation). */
  TripResponce reorderDayActivities(Long tripId, Integer dayId, List<Long> orderedActivityIds);

  /**
   * Replaces an activity's places with a mock substitute (first place in DB). Intended to be swapped for real
   * recommendation logic later.
   */
  TripResponce replaceActivityWithMockPlace(
      Long tripId, Long activityId, ReplaceActivityRequest request);
}
