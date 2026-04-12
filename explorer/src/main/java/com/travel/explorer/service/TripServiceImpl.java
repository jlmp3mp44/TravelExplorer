package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.google.GooglePlaceService;
import com.travel.explorer.google.geocode.GoogleGeocodingService;
import com.travel.explorer.google.geocode.LatLng;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripRepo;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
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

  private static final int ACTIVITIES_PER_DAY = 3;

  @Autowired
  private TripRepo tripRepo;

  @Autowired
  private PlaceRepo placeRepo;

  @Autowired
  ModelMapper modelMapper;

  @Autowired
  Random random;

  @Autowired
  private GooglePlaceService googlePlaceService;

  @Autowired
  private GoogleGeocodingService googleGeocodingService;

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
  @Transactional
  public TripResponce saveTrip(TriRequest triRequest) {

    Trip trip = modelMapper.map(triRequest, Trip.class);

    String geocodeAddress = buildGeocodeAddress(triRequest);
    LatLng center = googleGeocodingService.geocodeToLatLng(geocodeAddress);
    double radius = 10000.0;

    List<Place> generatedPlaces = googlePlaceService.searchNearby(
        center.latitude(), center.longitude(), radius);

    if (generatedPlaces != null && !generatedPlaces.isEmpty()) {
      List<Place> savedPlaces = new ArrayList<>();
      for (Place place : generatedPlaces) {
        savedPlaces.add(placeRepo.save(place));
      }

      int placeIndex = 0;
      for (LocalDate d = trip.getStartDate(); !d.isAfter(trip.getEndDate()); d = d.plusDays(1)) {
        Day day = new Day();
        day.setDate(d);
        day.setTrip(trip);

        for (int i = 0; i < ACTIVITIES_PER_DAY; i++) {
          Place place = savedPlaces.get(placeIndex % savedPlaces.size());
          placeIndex++;

          Activity activity = new Activity();
          activity.setDay(day);
          activity.setPlaces(List.of(place));
          day.getActivities().add(activity);
        }

        trip.getDays().add(day);
      }
    }

    trip.setTitle(generatetripTitle());

    tripRepo.save(trip);

    TripResponce tripResponce = modelMapper.map(trip, TripResponce.class);

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

  @Override
  public TripResponce getTripById(Long tripId) {
    Trip tripFromDb = tripRepo.findById(tripId)
        .orElseThrow(()-> new ResourceNotFoundException("Trip", "tripId", tripId));
    TripResponce tripResponce = modelMapper.map(tripFromDb, TripResponce.class);
    return tripResponce;
  }

  public String generatetripTitle(){
    return Integer.toString(random.nextInt(100, 100000));
  }

  private static String buildGeocodeAddress(TriRequest triRequest) {
    String cityPart = triRequest.getCity() != null ? triRequest.getCity().trim() : "";
    String countryPart =
        triRequest.getCountry() != null ? triRequest.getCountry().trim() : "";
    if (!cityPart.isEmpty() && !countryPart.isEmpty()) {
      return cityPart + ", " + countryPart;
    }
    if (!countryPart.isEmpty()) {
      return countryPart;
    }
    return cityPart;
  }
}
