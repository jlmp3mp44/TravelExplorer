package com.travel.explorer.service;

import com.travel.explorer.payload.trip.ReplaceActivityRequest;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripUpdateRequest;
import com.travel.explorer.payload.trip.TripResponce;
import java.util.List;

public interface TripService {

  TripListResponce getAllTrips(String sortBy, String sortOrder, Integer pageNumber, Integer pageSize);

  /**
   * Paginated trips owned by {@code ownerUserId}. Same sort/pagination shape as {@link #getAllTrips}.
   *
   * <p>If {@code viewerUserId} equals {@code ownerUserId} (logged-in owner viewing their profile),
   * both public and private trips are included. Otherwise only {@code isPublic == true} trips are
   * returned so private itineraries are not leaked.
   *
   * <p>Empty {@code content} means no matching rows (e.g. no owner on legacy trips, or no public
   * trips when viewing someone else).
   */
  TripListResponce getTripsForOwner(
      Long ownerUserId,
      Long viewerUserIdOrNull,
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize);

  TripResponce saveTrip(TriRequest triRequest, Long ownerUserId);

  TripResponce deleteTrip(Long tripId, Long currentUserId);

  TripResponce updateTrip(Long tripId, TripUpdateRequest request, Long currentUserId);

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
