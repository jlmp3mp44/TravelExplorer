package com.travel.explorer.controller;

import com.travel.explorer.config.AppConstants;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @GetMapping()
  public ResponseEntity<TripListResponce> getAllTrips(
      @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_TRIPS_BY, required = false) String sortBy,
      @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder,
      @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
      @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize
  ){
    return new ResponseEntity<>(tripService.getAllTrips(sortBy, sortOrder, pageNumber, pageSize), HttpStatus.OK);
  }

  @PostMapping()
  public ResponseEntity<TripResponce> saveTrip(@Valid @RequestBody TriRequest triRequest){
    TripResponce savedTrip = tripService.saveTrip(triRequest);
    return new ResponseEntity<>(savedTrip, HttpStatus.CREATED);
  }

  @DeleteMapping("{tripId}")
  public ResponseEntity<TripResponce> deleteTrip(@PathVariable Long tripId){
    TripResponce deletedTrip = tripService.deleteTrip(tripId);
    return new ResponseEntity<>(deletedTrip, HttpStatus.OK);
  }

  @PutMapping("{tripId}")
  public ResponseEntity<TripResponce> updateTrip(@PathVariable Long tripId, @RequestBody Trip trip){
    TripResponce updatedTrip = tripService.updateTrip(tripId, trip);
    return new ResponseEntity<>(updatedTrip, HttpStatus.OK);
  }

}
