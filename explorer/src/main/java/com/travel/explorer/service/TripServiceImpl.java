package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.google.GooglePlaceService;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripRepo;
import jakarta.transaction.Transactional;
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

  @Autowired
  private GooglePlaceService googlePlaceService;

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

    // 1. Мокаємо координати (Париж)
    double mockedLat = 48.8566;
    double mockedLng = 2.3522;
    double radius = 10000.0;

    // 2. Отримуємо місця з Google
    List<Place> generatedPlaces = googlePlaceService.searchNearby(mockedLat, mockedLng, radius);

    // Перевіряємо, чи Google взагалі щось повернув
    if (generatedPlaces != null && !generatedPlaces.isEmpty()) {

      // БЕРЕМО ЛИШЕ 1 ПЛЕЙС
      Place firstPlace = generatedPlaces.get(0);

      // Зберігаємо місце в базу, щоб воно отримало ID для зв'язку ManyToMany
      firstPlace = placeRepo.save(firstPlace);

      // 3. Створюємо 1 День (беремо дату початку подорожі)
      Day day = new Day();
      day.setDate(trip.getStartDate());
      day.setTrip(trip); // Зв'язуємо з Trip

      // 4. Створюємо 1 Активність
      Activity activity = new Activity();
      activity.setDay(day); // Зв'язуємо з Day
      activity.setPlaces(List.of(firstPlace)); // Додаємо наш 1 плейс

      // Додаємо активність у день
      day.getActivities().add(activity);

      // Додаємо день у подорож
      trip.getDays().add(day);
    }

    // 5. Генеруємо тайтл та зберігаємо всю конструкцію
    trip.setTitle(generatetripTitle());

    // Зберігаємо Trip (завдяки cascade збережуться і Day, і Activity)
    tripRepo.save(trip);

    // 6. Формуємо відповідь клієнту
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
}
