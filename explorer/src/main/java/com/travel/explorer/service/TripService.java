package com.travel.explorer.service;

import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;

public interface TripService {

  TripListResponce getAllTrips(String sortBy, String sortOrder, Integer pageNumber, Integer pageSize);

  TripResponce saveTrip(TriRequest triRequest);

  TripResponce deleteTrip(Long tripId);

  TripResponce updateTrip(Long tripId, Trip trip);
}
