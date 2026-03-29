package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripRepo;
import java.util.List;
import java.util.Random;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TripServiceImpl implements TripService{

  @Autowired
  private TripRepo tripRepo;

  @Autowired
  private PlaceRepo placeRepo;

  @Autowired
  ModelMapper modelMapper;

  @Autowired
  Random random;

  @Override
  public TripListResponce getAllTrips(String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
    Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();

    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    Page<Trip> tripPage =  tripRepo.findAll(pageDetails);
    List<Trip> trips = tripPage.getContent();

    if(trips.isEmpty()){
      throw new APIException("No trips created till now");
    }

    List<TripResponce> tripResponses = trips
        .stream()
        .map(trip -> {
          TripResponce resp = modelMapper.map(trip, TripResponce.class);
          return resp;
        })
        .toList();

    TripListResponce tripListResponce = new TripListResponce();
    tripListResponce.setContent(tripResponses);
    tripListResponce.setPageNumber(tripPage.getNumber());
    tripListResponce.setPageSize(tripPage.getSize());
    tripListResponce.setLastPage(tripPage.isLast());
    tripListResponce.setTotalPages(tripPage.getTotalPages());
    tripListResponce.setTotalElements(tripPage.getTotalElements());
    return tripListResponce;
  }


  @Override
  public TripResponce saveTrip(TriRequest triRequest){

    Trip trip = modelMapper.map(triRequest, Trip.class);
    //here should be logic of setting places

    trip.setTitle(generatetripTitle());
    tripRepo.save(trip);
    TripResponce tripResponce = modelMapper.map(trip, TripResponce.class);
    //tripResponce.setPlaceTitles(trip.getPlaces().stream().map(Place::getTitle).toList());
    return tripResponce;
  }

  @Override
  public TripResponce deleteTrip(Long tripId) {
    Trip trip = tripRepo.findById(tripId)
        .orElseThrow(()-> new ResourceNotFoundException("Trip", "tripId", tripId));
    TripResponce tripResponce = modelMapper.map(trip, TripResponce.class);
    tripRepo.deleteById(tripId);
    return tripResponce;
  }

  @Override
  public TripResponce updateTrip(Long tripId, Trip trip) {
    Trip existingTrip = tripRepo.findById(tripId)
        .orElseThrow(()-> new ResourceNotFoundException("Trip", "tripId", tripId));
    existingTrip.setTitle(trip.getTitle());
    existingTrip.setDesc(trip.getDesc());
    existingTrip.setStartDate(trip.getStartDate());
    existingTrip.setEndDate(trip.getEndDate());
    Trip savedTrip = tripRepo.save(existingTrip);
    TripResponce tripResponce = modelMapper.map(savedTrip, TripResponce.class);
    return tripResponce;
  }

  public String generatetripTitle(){
    return Integer.toString(random.nextInt(100, 100000));
  }
}
