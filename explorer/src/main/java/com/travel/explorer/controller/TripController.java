package com.travel.explorer.controller;

import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.TripDTO;
import com.travel.explorer.payload.TripListResponce;
import com.travel.explorer.payload.TripResponce;
import com.travel.explorer.service.TripService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/public/trips")
public class TripController {

  @Autowired
  TripService tripService;

  @GetMapping()
  public ResponseEntity<TripListResponce> getAllTrips(){
    return new ResponseEntity<>(tripService.getAllTrips(), HttpStatus.OK);
  }

  @PostMapping()
  public ResponseEntity<TripResponce> saveTrip(@Valid @RequestBody TripDTO tripDTO){
    TripResponce savedTrip = tripService.saveTrip(tripDTO);
    return new ResponseEntity<>(savedTrip, HttpStatus.CREATED);
  }

  @DeleteMapping("{tripId}")
  public ResponseEntity<Trip> deleteTrip(@PathVariable Long tripId){
    Trip deletedTrip = tripService.deleteTrip(tripId);
    return new ResponseEntity<>(deletedTrip, HttpStatus.OK);
  }

  @PutMapping("{tripId}")
  public ResponseEntity<Trip> updateTrip(@PathVariable Long tripId, @RequestBody Trip trip){
    Trip updatedTrip = tripService.updateTrip(tripId, trip);
    return new ResponseEntity<>(updatedTrip, HttpStatus.OK);
  }

}
