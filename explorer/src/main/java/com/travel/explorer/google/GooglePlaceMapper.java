package com.travel.explorer.google;

import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.GooglePlaceDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Service;

@Service
public class GooglePlaceMapper {

  private final ModelMapper modelMapper;
  private final CategoryResolutionService categoryResolutionService;

  public GooglePlaceMapper(ModelMapper modelMapper, CategoryResolutionService categoryResolutionService) {
    this.modelMapper = modelMapper;
    this.categoryResolutionService = categoryResolutionService;
    modelMapper.addMappings(
        new PropertyMap<GooglePlaceDto, Place>() {
          @Override
          protected void configure() {
            map().setTitle(source.getDisplayName().getText());
            map().setAddress(source.getFormattedAddress());
          }
        });
  }

  public Place toPlace(GooglePlaceDto dto) {
    Place place = modelMapper.map(dto, Place.class);
    place.setGooglePlaceId(dto.getGooglePlaceId());
    place.setCategories(categoryResolutionService.resolveFromGoogleTypes(dto.getTypes()));
    return place;
  }
}
