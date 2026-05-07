package com.travel.explorer.controller;

import com.travel.explorer.config.AppConstants;
import com.travel.explorer.payload.rating.RatingRequest;
import com.travel.explorer.payload.trip.ActivityManualEditRequest;
import com.travel.explorer.payload.trip.AddTripActivityRequest;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.payload.trip.ReplaceActivitySmartRequest;
import com.travel.explorer.payload.trip.ReplaceActivityWithPlaceRequest;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
      @RequestParam(name = "categoryCodes", required = false) List<String> categoryCodes,
      @RequestParam(name = "countryId", required = false) Long countryId,
      @RequestParam(name = "countryName", required = false) String countryName,
      Authentication authentication) {
    if (userId != null) {
      Long viewerId = currentUserId(authentication);
      TripListResponce list =
          tripService.getTripsForOwner(
              userId,
              viewerId,
              sortBy,
              sortOrder,
              pageNumber,
              pageSize,
              categoryCodes,
              countryId,
              countryName);
      return new ResponseEntity<>(list, HttpStatus.OK);
    }
    return new ResponseEntity<>(
        tripService.getAllTrips(
            sortBy,
            sortOrder,
            pageNumber,
            pageSize,
            categoryCodes,
            countryId,
            countryName),
        HttpStatus.OK);
  }

  @GetMapping(value = "{tripId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> downloadTripPdf(
      @PathVariable Long tripId, Authentication authentication) {
    byte[] pdf = tripService.exportTripAsPdf(tripId, currentUserId(authentication));
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"trip-" + tripId + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  @GetMapping("{tripId}")
  public ResponseEntity<TripResponce> getTripById(
      @PathVariable Long tripId,
      @RequestParam(required = false) Long userId,
      Authentication authentication) {
    TripResponce tripResponce =
        tripService.getTripById(tripId, userId, currentUserId(authentication));
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
      @RequestBody List<Long> orderedActivityIds,
      Authentication authentication) {
    TripResponce updated =
        tripService.reorderDayActivities(
            tripId, dayId, orderedActivityIds, currentUserId(authentication));
    return new ResponseEntity<>(updated, HttpStatus.OK);
  }

  @GetMapping("{tripId}/places/search")
  public ResponseEntity<List<PlaceResponse>> searchTripPlaces(
      @PathVariable Long tripId,
      @RequestParam("q") String query,
      Authentication authentication) {
    List<PlaceResponse> places =
        tripService.searchTripPlaces(tripId, query, currentUserId(authentication));
    return new ResponseEntity<>(places, HttpStatus.OK);
  }

  @PostMapping("{tripId}/activities/{activityId}/replace-smart")
  public ResponseEntity<TripResponce> replaceActivitySmart(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @RequestBody(required = false) ReplaceActivitySmartRequest request,
      Authentication authentication) {
    TripResponce updated =
        tripService.replaceActivitySmart(
            tripId, activityId, request != null ? request : new ReplaceActivitySmartRequest(), currentUserId(authentication));
    return new ResponseEntity<>(updated, HttpStatus.OK);
  }

  @PostMapping("{tripId}/activities/{activityId}/replace-with-place")
  public ResponseEntity<TripResponce> replaceActivityWithPlace(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @Valid @RequestBody ReplaceActivityWithPlaceRequest request,
      Authentication authentication) {
    TripResponce updated =
        tripService.replaceActivityWithPlace(
            tripId, activityId, request, currentUserId(authentication));
    return new ResponseEntity<>(updated, HttpStatus.OK);
  }

  @DeleteMapping("{tripId}/activities/{activityId}")
  public ResponseEntity<TripResponce> deleteTripActivity(
      @PathVariable Long tripId,
      @PathVariable Long activityId,
      @Valid @RequestBody ActivityManualEditRequest request,
      Authentication authentication) {
    TripResponce updated =
        tripService.deleteTripActivity(
            tripId, activityId, request, currentUserId(authentication));
    return new ResponseEntity<>(updated, HttpStatus.OK);
  }

  @PostMapping("{tripId}/days/{dayId}/activities/auto")
  public ResponseEntity<TripResponce> addTripActivityAuto(
      @PathVariable Long tripId,
      @PathVariable Integer dayId,
      Authentication authentication) {
    TripResponce updated =
        tripService.addTripActivityAuto(tripId, dayId, currentUserId(authentication));
    return new ResponseEntity<>(updated, HttpStatus.CREATED);
  }

  @PostMapping("{tripId}/days/{dayId}/activities")
  public ResponseEntity<TripResponce> addTripActivity(
      @PathVariable Long tripId,
      @PathVariable Integer dayId,
      @RequestBody(required = false) AddTripActivityRequest request,
      Authentication authentication) {
    Long userId = currentUserId(authentication);
    TripResponce updated;
    if (request == null || request.getPlaceId() == null) {
      // Compatibility fallback: clients can trigger "Suggest for me" without a body.
      updated = tripService.addTripActivityAuto(tripId, dayId, userId);
    } else {
      updated = tripService.addTripActivity(tripId, dayId, request, userId);
    }
    return new ResponseEntity<>(updated, HttpStatus.CREATED);
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
