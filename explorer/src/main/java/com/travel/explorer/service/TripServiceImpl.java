package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.TripDTO;
import com.travel.explorer.payload.TripListResponce;
import com.travel.explorer.payload.TripResponce;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripRepo;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TripServiceImpl implements TripService{

  @Autowired
  private TripRepo tripRepo;

  @Autowired
  private PlaceRepo placeRepo;

  @Autowired
  ModelMapper modelMapper;

  @Override
  public TripListResponce getAllTrips(){

    List<TripResponce> tripResponces = tripRepo.findAll()
        .stream().map((t)-> modelMapper.map(t, TripResponce.class)).toList();
    TripListResponce tripListResponce = new TripListResponce();
    tripListResponce.setContent(tripResponces);
    return tripListResponce;
  }

  @Override
  public TripResponce saveTrip(TripDTO tripDTO){

    Trip trip = modelMapper.map(tripDTO, Trip.class);
    List<Place> places = placeRepo.findAllById(tripDTO.getPlaceIds());
    if (places.size() != tripDTO.getPlaceIds().size()) {
      throw new APIException("Not all places exists");
    }
    trip.setPlaces(places);

    tripRepo.save(trip);
    TripResponce tripResponce = modelMapper.map(trip, TripResponce.class);
    tripResponce.setPlaceTitles(trip.getPlaces().stream().map(Place::getTitle).toList());
    return tripResponce;
  }

  @Override
  public Trip deleteTrip(Long tripId) {
    Trip trip = tripRepo.findById(tripId)
        .orElseThrow(()-> new ResourceNotFoundException("Trip", "tripId", tripId));
    tripRepo.deleteById(tripId);
    return trip;
  }

  @Override
  public Trip updateTrip(Long tripId, Trip trip) {
    Trip existingTrip = tripRepo.findById(tripId)
        .orElseThrow(()-> new ResourceNotFoundException("Trip", "tripId", tripId));
    existingTrip.setTitle(trip.getTitle());
    existingTrip.setDesc(trip.getDesc());
    existingTrip.setStartDate(trip.getStartDate());
    existingTrip.setEndDate(trip.getEndDate());
    existingTrip.setPlaces(trip.getPlaces());
    Trip savedTrip = tripRepo.save(existingTrip);
    return savedTrip;
  }
}
