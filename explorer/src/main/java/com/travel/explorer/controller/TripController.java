package com.travel.explorer.controller;

import com.travel.explorer.config.AppConstants;
import com.travel.explorer.payload.rating.RatingRequest;
import com.travel.explorer.payload.trip.ReplaceActivityRequest;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.payload.trip.TripUpdateRequest;
import com.travel.explorer.security.service.UserDetailsImpl;
import com.travel.explorer.service.RatingService;
import com.travel.explorer.service.TripService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/public/trips")
public class TripController {

  @Autowired
  TripService tripService;

  @Autowired
  RatingService ratingService;

  @GetMapping()
  public ResponseEntity<TripListResponce> getAllTrips(
      @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_TRIPS_BY, required = false) String sortBy,
      @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder,
      @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
      @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
      @RequestParam(name = "userId", required = false) Long userId,
      Authentication authentication) {
    if (userId != null) {
      Long viewerId = currentUserId(authentication);
      TripListResponce list =
          tripService.getTripsForOwner(userId, viewerId, sortBy, sortOrder, pageNumber, pageSize);
      return new ResponseEntity<>(list, HttpStatus.OK);
    }
    return new ResponseEntity<>(
        tripService.getAllTrips(sortBy, sortOrder, pageNumber, pageSize), HttpStatus.OK);
  }

  @GetMapping("{tripId}")
  public ResponseEntity<TripResponce> getTripById(
      @PathVariable Long tripId, @RequestParam(required = false) Long userId) {
    TripResponce tripResponce = tripService.getTripById(tripId, userId);
    return new ResponseEntity<>(tripResponce, HttpStatus.OK);
  }

  @PostMapping()
  public ResponseEntity<TripResponce> saveTrip(
      @Valid @RequestBody TriRequest triRequest, Authentication authentication) {
    Long ownerId = currentUserId(authentication);
    TripResponce savedTrip = tripService.saveTrip(triRequest, ownerId);
    return new ResponseEntity<>(savedTrip, HttpStatus.CREATED);
  }

  @DeleteMapping("{tripId}")
  public ResponseEntity<TripResponce> deleteTrip(
      @PathVariable Long tripId, Authentication authentication) {
    TripResponce deletedTrip = tripService.deleteTrip(tripId, currentUserId(authentication));
    return new ResponseEntity<>(deletedTrip, HttpStatus.OK);
  }

  @PutMapping("{tripId}")
  public ResponseEntity<TripResponce> updateTrip(
      @PathVariable Long tripId,
      @Valid @RequestBody TripUpdateRequest request,
      Authentication authentication) {
    TripResponce updatedTrip =
        tripService.updateTrip(tripId, request, currentUserId(authentication));
    return new ResponseEntity<>(updatedTrip, HttpStatus.OK);
  }

  @PutMapping("{tripId}/days/{dayId}/activities/order")
  public ResponseEntity<TripResponce> reorderDayActivities(
      @PathVariable Long tripId,
      @PathVariable Integer dayId,
      @RequestBody List<Long> orderedActivityIds) {
    TripResponce updated = tripService.reorderDayActivities(tripId, dayId, orderedActivityIds);
    return new ResponseEntity<>(updated, HttpStatus.OK);
  }

  @PostMapping("{tripId}/activities/{activityId}/replace")
  public ResponseEntity<TripResponce> replaceActivityWithMockPlace(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @Valid @RequestBody ReplaceActivityRequest request) {
    TripResponce updated = tripService.replaceActivityWithMockPlace(tripId, activityId, request);
    return new ResponseEntity<>(updated, HttpStatus.OK);
  }

  @PostMapping("{tripId}/ratings")
  public ResponseEntity<Void> rateTrip(
      @PathVariable Long tripId, @Valid @RequestBody RatingRequest request) {
    ratingService.rateTrip(tripId, request.getUserId(), request.getStars());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @PostMapping("{tripId}/activities/{activityId}/ratings")
  public ResponseEntity<Void> rateActivity(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @Valid @RequestBody RatingRequest request) {
    ratingService.rateActivity(tripId, activityId, request.getUserId(), request.getStars());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private static Long currentUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
      return null;
    }
    return ((UserDetailsImpl) authentication.getPrincipal()).getId();
  }

}
