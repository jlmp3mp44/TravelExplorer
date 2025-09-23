package com.travel.explorer.controller;


import com.travel.explorer.entities.Place;
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
  public ResponseEntity<List<Place>> getAllPlaces(){
    return new ResponseEntity<>(placeService.getAllRlaces(), HttpStatus.OK);
  }

  @PostMapping()
  public ResponseEntity<Place> savePlace(@Valid  @RequestBody  Place place){
    Place savedPlace = placeService.savePlace(place);
    return new ResponseEntity<>(savedPlace, HttpStatus.CREATED);
  }

  @DeleteMapping("{placeId}")
  public ResponseEntity<Place> deletePlace(@PathVariable Long placeId){
    Place deletedPlace = placeService.deletePlace(placeId);
    return new ResponseEntity<>(deletedPlace, HttpStatus.NO_CONTENT);
  }

  @PutMapping("{placeId}")
  public ResponseEntity<Place> updatePlace(@PathVariable Long placeId, @RequestBody Place place){
    Place updatedPlace = placeService.updatePlace(placeId, place);
    return new ResponseEntity<>(updatedPlace, HttpStatus.OK);
  }

}
