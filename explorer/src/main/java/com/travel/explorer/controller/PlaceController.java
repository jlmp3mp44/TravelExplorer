package com.travel.explorer.controller;


import com.travel.explorer.config.AppConstants;
import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.PlaceListResponse;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.service.PlaceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("api/public/places")
public class PlaceController {

  @Autowired
  PlaceService placeService;

  @GetMapping()
  public ResponseEntity<PlaceListResponse> getAllPlaces(
      @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PLACES_BY, required = false) String sortBy,
      @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder,
      @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
      @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize
  ){
    return new ResponseEntity<>(placeService.getAllPlaces(sortBy, sortOrder, pageNumber, pageSize), HttpStatus.OK);
  }

  @PostMapping()
  public ResponseEntity<PlaceResponse> savePlace(@Valid  @RequestBody  Place place){
    PlaceResponse savedPlace = placeService.savePlace(place);
    return new ResponseEntity<>(savedPlace, HttpStatus.CREATED);
  }

  @DeleteMapping("{placeId}")
  public ResponseEntity<PlaceResponse> deletePlace(@PathVariable Long placeId){
    PlaceResponse deletedPlace = placeService.deletePlace(placeId);
    return new ResponseEntity<>(deletedPlace, HttpStatus.OK);
  }

  @PutMapping("{placeId}")
  public ResponseEntity<PlaceResponse> updatePlace(@PathVariable Long placeId, @RequestBody Place place){
    PlaceResponse updatedPlace = placeService.updatePlace(placeId, place);
    return new ResponseEntity<>(updatedPlace, HttpStatus.OK);
  }

}
