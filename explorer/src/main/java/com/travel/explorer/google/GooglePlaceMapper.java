package com.travel.explorer.google;

import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.GooglePlaceDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Service;

@Service
public class GooglePlaceMapper {

  private final ModelMapper modelMapper;

  public GooglePlaceMapper() {
    this.modelMapper = new ModelMapper();

    modelMapper.addMappings(new PropertyMap<GooglePlaceDto, Place>() {
      @Override
      protected void configure() {
        map().setTitle(source.getDisplayName().getText());
        map().setAddress(source.getFormattedAddress());
      }
    });
  }

  public Place toPlace(GooglePlaceDto dto) {
    return modelMapper.map(dto, Place.class);
  }
}