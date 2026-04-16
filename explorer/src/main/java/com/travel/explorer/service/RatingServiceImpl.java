package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.ActivityRating;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.TripRating;
import com.travel.explorer.entities.User;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.ActivityResponse;
import com.travel.explorer.payload.DayResponse;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.repo.ActivityRatingRepository;
import com.travel.explorer.repo.ActivityRepository;
import com.travel.explorer.repo.TripRatingRepository;
import com.travel.explorer.repo.TripRepo;
import com.travel.explorer.repo.UserRepository;
import java.util.List;
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
  private ActivityRatingRepository activityRatingRepository;

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
            .findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", activityId));

    if (activity.getDay() == null
        || activity.getDay().getTrip() == null
        || !activity.getDay().getTrip().getId().equals(trip.getId())) {
      throw new APIException("Activity does not belong to this trip");
    }

    ActivityRating rating =
        activityRatingRepository
            .findByUser_UserIdAndActivity_Id(userId, activityId)
            .orElseGet(
                () -> {
                  ActivityRating ar = new ActivityRating();
                  ar.setUser(user);
                  ar.setActivity(activity);
                  return ar;
                });
    rating.setStars(stars);
    activityRatingRepository.save(rating);
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
        activityRatingRepository
            .averageStarsByActivityId(ar.getId())
            .ifPresentOrElse(ar::setAverageRating, () -> ar.setAverageRating(null));
        ar.setRatingCount(activityRatingRepository.countByActivity_Id(ar.getId()));
      }
    }
  }
}
