package com.travel.explorer.controller;

import com.travel.explorer.entities.City;
import com.travel.explorer.entities.Country;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.location.CityOptionResponse;
import com.travel.explorer.payload.location.CountryOptionResponse;
import com.travel.explorer.repo.CityRepository;
import com.travel.explorer.repo.CountryRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/public")
public class LocationController {

  @Autowired
  private CountryRepository countryRepository;

  @Autowired
  private CityRepository cityRepository;

  @GetMapping("/countries")
  public ResponseEntity<List<CountryOptionResponse>> getCountries() {
    List<CountryOptionResponse> body = countryRepository.findAllByOrderByNameAsc().stream()
        .map(this::toCountryOption)
        .toList();
    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  @GetMapping("/countries/{countryId}/cities")
  public ResponseEntity<List<CityOptionResponse>> getCitiesByCountry(
      @PathVariable Long countryId) {
    if (!countryRepository.existsById(countryId)) {
      throw new ResourceNotFoundException("Country", "countryId", countryId);
    }
    List<CityOptionResponse> body = cityRepository.findByCountry_IdOrderByNameAsc(countryId).stream()
        .map(this::toCityOption)
        .toList();
    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  private CountryOptionResponse toCountryOption(Country c) {
    return new CountryOptionResponse(c.getId(), c.getName(), c.getIso());
  }

  private CityOptionResponse toCityOption(City city) {
    Long cid = city.getCountry() != null ? city.getCountry().getId() : null;
    return new CityOptionResponse(city.getId(), city.getName(), cid);
  }
}
