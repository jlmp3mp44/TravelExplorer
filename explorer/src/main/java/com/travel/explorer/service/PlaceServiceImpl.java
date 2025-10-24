package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.TripListResponce;
import com.travel.explorer.payload.TripResponce;
import com.travel.explorer.payload.place.PlaceListResponse;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.repo.PlaceRepo;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PlaceServiceImpl implements PlaceService{
  @Autowired
  private PlaceRepo placeRepo;

  @Autowired
  private ModelMapper modelMapper;

  @Override
  public PlaceListResponse getAllPlaces(String sortBy, String sortOrder,  Integer pageNumber, Integer pageSize){
    Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();

    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    Page<Place> placePage =  placeRepo.findAll(pageDetails);
    List<Place> places = placePage.getContent();

    List<PlaceResponse> placeResponses = places
        .stream()
        .map(place -> {
          PlaceResponse resp = modelMapper.map(place, PlaceResponse.class);
          return resp;
        })
        .toList();

    PlaceListResponse placeListResponce = new PlaceListResponse();
    placeListResponce.setContent(placeResponses);
    placeListResponce.setPageNumber(placePage.getNumber());
    placeListResponce.setPageSize(placePage.getSize());
    placeListResponce.setLastPage(placePage.isLast());
    placeListResponce.setTotalPages(placePage.getTotalPages());
    placeListResponce.setTotalElements(placePage.getTotalElements());
    return placeListResponce;
  }

  @Override
  public PlaceResponse savePlace(Place place){
    Optional<Place> existingPlace = placeRepo.findByTitle(place.getTitle());
    if(existingPlace.isPresent()){
      throw  new APIException("Place with title " + place.getTitle() + " is already exists!");
    }
    return modelMapper.map(placeRepo.save(place), PlaceResponse.class);
  }

  @Override
  public PlaceResponse deletePlace(Long placeId) {
    Place place = placeRepo.findById(placeId)
            .orElseThrow(()-> new ResourceNotFoundException("Place", "placeId", placeId));
    PlaceResponse placeResponse = modelMapper.map(place, PlaceResponse.class);
    placeRepo.deleteById(placeId);
    return placeResponse;
  }

  @Override
  public PlaceResponse updatePlace(Long placeId, Place place) {
    Place existingPlace = placeRepo.findById(placeId)
        .orElseThrow(()-> new ResourceNotFoundException("Place", "placeId", placeId));
    existingPlace.setTitle(place.getTitle());
    existingPlace.setDesc(place.getDesc());
    existingPlace.setTrips(place.getTrips());
    existingPlace.setPhoto(place.getPhoto());
    Place savedPlace = placeRepo.save(existingPlace);
    return modelMapper.map(savedPlace, PlaceResponse.class);
  }

}
