package com.travel.explorer.controller;

import com.travel.explorer.config.AppConstants;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.security.service.UserDetailsImpl;
import com.travel.explorer.service.TripService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public profile-style listing: another user's trips (public only unless you are that user).
 */
@RestController
@RequestMapping("api/public/users")
public class PublicUserTripController {

  @Autowired
  private TripService tripService;

  @GetMapping("{userId}/trips")
  public ResponseEntity<TripListResponce> listTripsForUser(
      @PathVariable Long userId,
      @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_TRIPS_BY, required = false) String sortBy,
      @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder,
      @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)
          Integer pageNumber,
      @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
      @RequestParam(name = "categoryCodes", required = false) List<String> categoryCodes,
      @RequestParam(name = "countryId", required = false) Long countryId,
      @RequestParam(name = "countryName", required = false) String countryName,
      Authentication authentication) {
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

  private static Long currentUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
      return null;
    }
    return ((UserDetailsImpl) authentication.getPrincipal()).getId();
  }
}
