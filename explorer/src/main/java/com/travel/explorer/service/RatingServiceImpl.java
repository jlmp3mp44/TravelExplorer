package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.PlaceRating;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.TripRating;
import com.travel.explorer.entities.User;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.ActivityResponse;
import com.travel.explorer.payload.DayResponse;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.repo.ActivityRepository;
import com.travel.explorer.repo.PlaceRatingRepository;
import com.travel.explorer.repo.TripRatingRepository;
import com.travel.explorer.repo.TripRepo;
import com.travel.explorer.repo.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingServiceImpl implements RatingService {

  @Autowired
  private TripRepo tripRepo;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ActivityRepository activityRepository;

  @Autowired
  private TripRatingRepository tripRatingRepository;

  @Autowired
  private PlaceRatingRepository placeRatingRepository;

  @Override
  @Transactional
  public void rateTrip(Long tripId, Long userId, int stars) {
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

    TripRating rating =
        tripRatingRepository
            .findByUser_UserIdAndTrip_Id(userId, tripId)
            .orElseGet(
                () -> {
                  TripRating tr = new TripRating();
                  tr.setUser(user);
                  tr.setTrip(trip);
                  return tr;
                });
    rating.setStars(stars);
    tripRatingRepository.save(rating);
  }

  @Override
  @Transactional
  public void rateActivity(Long tripId, Long activityId, Long userId, int stars) {
    if (stars < 1 || stars > 5) {
      throw new APIException("stars must be between 1 and 5");
    }
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    Activity activity =
        activityRepository
            .findByIdWithPlaces(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", activityId));

    if (activity.getDay() == null
        || activity.getDay().getTrip() == null
        || !activity.getDay().getTrip().getId().equals(trip.getId())) {
      throw new APIException("Activity does not belong to this trip");
    }

    Place place = primaryPlaceForRating(activity);
    if (place == null || place.getId() == null) {
      throw new APIException("Activity has no place to rate");
    }

    PlaceRating rating =
        placeRatingRepository
            .findByUser_UserIdAndPlace_Id(userId, place.getId())
            .orElseGet(
                () -> {
                  PlaceRating pr = new PlaceRating();
                  pr.setUser(user);
                  pr.setPlace(place);
                  return pr;
                });
    rating.setStars(stars);
    placeRatingRepository.save(rating);
  }

  /** First place on the activity (typical case: one place per stop). */
  private static Place primaryPlaceForRating(Activity activity) {
    if (activity.getPlaces() == null || activity.getPlaces().isEmpty()) {
      return null;
    }
    return activity.getPlaces().get(0);
  }

  private static Long primaryPlaceIdFromResponse(ActivityResponse ar) {
    if (ar.getPlaces() == null) {
      return null;
    }
    for (PlaceResponse p : ar.getPlaces()) {
      if (p != null && p.getId() != null) {
        return p.getId();
      }
    }
    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public void attachRatingSummaries(TripResponce tripResponce) {
    if (tripResponce == null || tripResponce.getId() == null) {
      return;
    }
    Long tripId = tripResponce.getId().longValue();

    tripRatingRepository
        .averageStarsByTripId(tripId)
        .ifPresentOrElse(
            avg -> tripResponce.setAverageRating(avg),
            () -> tripResponce.setAverageRating(null));
    tripResponce.setRatingCount(tripRatingRepository.countByTrip_Id(tripId));

    List<DayResponse> days = tripResponce.getDays();
    if (days == null) {
      return;
    }
    for (DayResponse day : days) {
      if (day.getActivities() == null) {
        continue;
      }
      for (ActivityResponse ar : day.getActivities()) {
        if (ar.getId() == null) {
          continue;
        }
        Long placeId = primaryPlaceIdFromResponse(ar);
        if (placeId == null) {
          ar.setAverageRating(null);
          ar.setRatingCount(0);
          continue;
        }
        placeRatingRepository
            .averageStarsByPlaceId(placeId)
            .ifPresentOrElse(ar::setAverageRating, () -> ar.setAverageRating(null));
        ar.setRatingCount(placeRatingRepository.countByPlace_Id(placeId));
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void attachTripListRatingSummaries(List<TripResponce> trips) {
    if (trips == null || trips.isEmpty()) {
      return;
    }
    List<Long> tripIds =
        trips.stream()
            .filter(t -> t != null && t.getId() != null)
            .map(t -> t.getId().longValue())
            .distinct()
            .toList();
    if (tripIds.isEmpty()) {
      return;
    }

    Map<Long, TripRatingStats> statsByTripId = new HashMap<>();
    for (Object[] row : tripRatingRepository.aggregateRatingStatsByTripIds(tripIds)) {
      Long tripId = (Long) row[0];
      Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : null;
      long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
      statsByTripId.put(tripId, new TripRatingStats(avg, count));
    }

    for (TripResponce trip : trips) {
      if (trip == null || trip.getId() == null) {
        continue;
      }
      TripRatingStats stats = statsByTripId.get(trip.getId().longValue());
      if (stats == null || stats.count() == 0) {
        trip.setAverageRating(null);
        trip.setRatingCount(0);
      } else {
        trip.setAverageRating(stats.average());
        trip.setRatingCount(stats.count());
      }
    }
  }

  private record TripRatingStats(Double average, long count) {}
}
