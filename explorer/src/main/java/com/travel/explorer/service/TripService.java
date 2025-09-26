package com.travel.explorer.service;

import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.TripDTO;
import com.travel.explorer.payload.TripListResponce;
import com.travel.explorer.payload.TripResponce;
import java.util.List;

public interface TripService {

  TripListResponce getAllTrips();

  TripResponce saveTrip(TripDTO tripDTO);

  Trip deleteTrip(Long tripId);

  Trip updateTrip(Long tripId, Trip trip);
}
