package com.travel.explorer.google;

import com.travel.explorer.entities.Location;
import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.GooglePlaceDto;
import java.util.Locale;
import java.util.Map;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Service;

@Service
public class GooglePlaceMapper {

  private final ModelMapper modelMapper;
  private final CategoryResolutionService categoryResolutionService;

  private static final Map<String, Integer> PRICE_LEVEL_MAP = Map.of(
      "PRICE_LEVEL_FREE", 0,
      "PRICE_LEVEL_INEXPENSIVE", 1,
      "PRICE_LEVEL_MODERATE", 2,
      "PRICE_LEVEL_EXPENSIVE", 3,
      "PRICE_LEVEL_VERY_EXPENSIVE", 4
  );

  public GooglePlaceMapper(ModelMapper modelMapper, CategoryResolutionService categoryResolutionService) {
    this.modelMapper = modelMapper;
    this.categoryResolutionService = categoryResolutionService;
    modelMapper.addMappings(
        new PropertyMap<GooglePlaceDto, Place>() {
          @Override
          protected void configure() {
            // Default tokenized matching can pair googlePlaceId with id (both contain "id"),
            // then NumberConverter fails on values like "ChIJ…".
            skip(destination.getId());
            skip(destination.getLocation());
            skip(destination.getCategories());
            map().setTitle(source.getDisplayName().getText());
            map().setAddress(source.getFormattedAddress());
            map().setPrimaryType(source.getPrimaryType());
            map().setBusinessStatus(source.getBusinessStatus());
            map().setRating(source.getRating());
            map().setUserRatingCount(source.getUserRatingCount());
            skip(destination.getPriceLevel());
          }
        });
  }

  public Place toPlace(GooglePlaceDto dto) {
    Place place = modelMapper.map(dto, Place.class);
    place.setGooglePlaceId(dto.getGooglePlaceId());
    if (dto.getLocation() != null) {
      Location location = new Location();
      location.setLat(dto.getLocation().latitude());
      location.setLng(dto.getLocation().longitude());
      place.setLocation(location);
    }
    place.setCategories(categoryResolutionService.resolveFromGoogleTypes(dto.getTypes()));
    place.setPriceLevel(parsePriceLevel(dto.getPriceLevel()));
    place.setPermanentlyClosed(isBusinessStatus(dto.getBusinessStatus(), "CLOSED_PERMANENTLY"));
    place.setTemporarilyClosed(isBusinessStatus(dto.getBusinessStatus(), "CLOSED_TEMPORARILY"));
    return place;
  }

  /** Converts Google API enum string (e.g. "PRICE_LEVEL_MODERATE") to integer (0-4). */
  private static Integer parsePriceLevel(String priceLevel) {
    if (priceLevel == null || priceLevel.isBlank()) {
      return null;
    }
    return PRICE_LEVEL_MAP.get(priceLevel.trim().toUpperCase(Locale.ROOT));
  }

  private static boolean isBusinessStatus(String status, String expected) {
    return status != null && expected.equals(status.trim().toUpperCase(Locale.ROOT));
  }
}
