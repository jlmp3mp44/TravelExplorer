package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.repo.PlaceRepo;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService{
  @Autowired
  private PlaceRepo placeRepo;

  public List<Place> getAllRlaces(){
    return placeRepo.findAll();
  }

  public Place savePlace(Place place){
    Optional<Place> existingPlace = placeRepo.findByTitle(place.getTitle());
    if(existingPlace.isPresent()){
      throw  new APIException("Place with title " + place.getTitle() + " is already exists!");
    }
    return placeRepo.save(place);
  }

  @Override
  public Place deletePlace(Long placeId) {
    Place place = placeRepo.findById(placeId)
            .orElseThrow(()-> new ResourceNotFoundException("Place", "placeId", placeId));
    placeRepo.deleteById(placeId);
    return place;
  }

  @Override
  public Place updatePlace(Long placeId, Place place) {
    Place existingPlace = placeRepo.findById(placeId)
        .orElseThrow(()-> new ResourceNotFoundException("Place", "placeId", placeId));
    existingPlace.setTitle(place.getTitle());
    existingPlace.setDesc(place.getDesc());
    existingPlace.setTrips(place.getTrips());
    existingPlace.setPhoto(place.getPhoto());
    Place savedPlace = placeRepo.save(existingPlace);
    return savedPlace;
  }

}
