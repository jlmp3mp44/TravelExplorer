package com.travel.explorer.service;

import com.travel.explorer.payload.trip.TripResponce;
import java.util.List;

public interface RatingService {

  void rateTrip(Long tripId, Long userId, int stars);

  void rateActivity(Long tripId, Long activityId, Long userId, int stars);

  void attachRatingSummaries(TripResponce tripResponce);

  /** Trip-level ratings only (for paginated trip lists). */
  void attachTripListRatingSummaries(List<TripResponce> trips);
}
