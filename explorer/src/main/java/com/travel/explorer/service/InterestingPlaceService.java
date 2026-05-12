package com.travel.explorer.service;

import com.travel.explorer.entities.City;
import com.travel.explorer.entities.Country;
import com.travel.explorer.entities.InterestingPlace;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.User;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.place.InterestingPlaceResponse;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.payload.place.SaveInterestingPlaceRequest;
import com.travel.explorer.repo.CityRepository;
import com.travel.explorer.repo.CountryRepository;
import com.travel.explorer.repo.InterestingPlaceRepository;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterestingPlaceService {

  @Autowired private InterestingPlaceRepository repo;
  @Autowired private UserRepository userRepository;
  @Autowired private PlaceRepo placeRepo;
  @Autowired private CityRepository cityRepository;
  @Autowired private CountryRepository countryRepository;
  @Autowired private ModelMapper modelMapper;

  @Transactional(readOnly = true)
  public List<InterestingPlaceResponse> list(Long userId) {
    requireUserId(userId);
    return repo.findAllByUser_UserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public InterestingPlaceResponse save(Long userId, SaveInterestingPlaceRequest request) {
    requireUserId(userId);
    if (request == null || request.getPlaceId() == null) {
      throw new APIException("placeId is required");
    }
    Place place =
        placeRepo
            .findById(request.getPlaceId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Place", "placeId", request.getPlaceId()));

    return repo.findByUser_UserIdAndPlace_Id(userId, place.getId())
        .map(this::toResponse)
        .orElseGet(
            () -> {
              User user =
                  userRepository
                      .findById(userId)
                      .orElseThrow(() -> new APIException("User not found"));
              InterestingPlace ip = new InterestingPlace();
              ip.setUser(user);
              ip.setPlace(place);
              if (request.getCityId() != null) {
                City city =
                    cityRepository
                        .findById(request.getCityId())
                        .orElseThrow(
                            () ->
                                new ResourceNotFoundException(
                                    "City", "cityId", request.getCityId()));
                ip.setCity(city);
                if (city.getCountry() != null && request.getCountryId() == null) {
                  ip.setCountry(city.getCountry());
                }
              }
              if (request.getCountryId() != null && ip.getCountry() == null) {
                Country country =
                    countryRepository
                        .findById(request.getCountryId())
                        .orElseThrow(
                            () ->
                                new ResourceNotFoundException(
                                    "Country", "countryId", request.getCountryId()));
                ip.setCountry(country);
              }
              return toResponse(repo.save(ip));
            });
  }

  @Transactional
  public void delete(Long userId, Long id) {
    requireUserId(userId);
    InterestingPlace ip =
        repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("InterestingPlace", "id", id));
    if (ip.getUser() == null || !userId.equals(ip.getUser().getUserId())) {
      throw new APIException("Not allowed");
    }
    repo.delete(ip);
  }

  /**
   * Returns saved-place matches for the trip context. If {@code cityId != null}, matches by city
   * only; otherwise matches by country.
   */
  @Transactional(readOnly = true)
  public List<InterestingPlaceResponse> findMatching(Long userId, Long cityId, Long countryId) {
    requireUserId(userId);
    List<InterestingPlace> matches;
    if (cityId != null) {
      matches = repo.findAllByUser_UserIdAndCity_IdIn(userId, List.of(cityId));
    } else if (countryId != null) {
      matches = repo.findAllByUser_UserIdAndCountry_Id(userId, countryId);
    } else {
      matches = new ArrayList<>();
    }
    return matches.stream().map(this::toResponse).toList();
  }

  private void requireUserId(Long userId) {
    if (userId == null) {
      throw new APIException("Sign in is required");
    }
  }

  private InterestingPlaceResponse toResponse(InterestingPlace ip) {
    InterestingPlaceResponse r = new InterestingPlaceResponse();
    r.setId(ip.getId());
    r.setCreatedAt(ip.getCreatedAt());
    if (ip.getPlace() != null) {
      r.setPlace(modelMapper.map(ip.getPlace(), PlaceResponse.class));
    }
    if (ip.getCity() != null) {
      r.setCityId(ip.getCity().getId());
      r.setCityName(ip.getCity().getName());
    }
    if (ip.getCountry() != null) {
      r.setCountryId(ip.getCountry().getId());
      r.setCountryName(ip.getCountry().getName());
    }
    return r;
  }
}
