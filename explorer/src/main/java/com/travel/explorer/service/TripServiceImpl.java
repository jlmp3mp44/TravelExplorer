package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.City;
import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.User;
import com.travel.explorer.entities.UserActivityPreference;
import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.google.GooglePlaceService;
import com.travel.explorer.google.geocode.GoogleGeocodingService;
import com.travel.explorer.google.geocode.LatLng;
import com.travel.explorer.payload.ActivityResponse;
import com.travel.explorer.payload.ActivityUserPreferenceResponse;
import com.travel.explorer.payload.DayResponse;
import com.travel.explorer.payload.place.PlaceResponse;
import com.travel.explorer.payload.trip.ReplaceActivityRequest;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.payload.trip.TripUpdateRequest;
import com.travel.explorer.repo.ActivityRepository;
import com.travel.explorer.repo.CityRepository;
import com.travel.explorer.repo.DayRepository;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripRepo;
import com.travel.explorer.repo.UserActivityPreferenceRepository;
import com.travel.explorer.repo.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TripServiceImpl implements TripService {

  private static final int ACTIVITIES_PER_DAY = 3;

  @Autowired
  private TripRepo tripRepo;

  @Autowired
  private CityRepository cityRepository;

  @Autowired
  private PlaceRepo placeRepo;

  @Autowired
  private DayRepository dayRepository;

  @Autowired
  private ActivityRepository activityRepository;

  @Autowired
  private RatingService ratingService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserActivityPreferenceRepository userActivityPreferenceRepository;

  @Autowired
  ModelMapper modelMapper;

  @Autowired
  private GooglePlaceService googlePlaceService;

  @Autowired
  private GoogleGeocodingService googleGeocodingService;

  @Autowired
  private PlaceRecommendationService placeRecommendationService;

  @Override
  public TripListResponce getAllTrips(
      String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
    Page<Trip> tripPage = pagedTrips(null, sortBy, sortOrder, pageNumber, pageSize);
    if (tripPage.getContent().isEmpty()) {
      throw new APIException("No trips created till now");
    }
    return toTripListResponse(tripPage);
  }

  @Override
  public TripListResponce getTripsForOwner(
      Long ownerUserId,
      Long viewerUserIdOrNull,
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize) {
    boolean ownerViewingSelf =
        viewerUserIdOrNull != null && viewerUserIdOrNull.equals(ownerUserId);
    Page<Trip> tripPage =
        pagedTripsForOwner(ownerUserId, ownerViewingSelf, sortBy, sortOrder, pageNumber, pageSize);
    return toTripListResponse(tripPage);
  }

  private Page<Trip> pagedTrips(
      Long ownerUserIdOrNull, String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    if (ownerUserIdOrNull == null) {
      return tripRepo.findAll(pageDetails);
    }
    return tripRepo.findByOwner_UserId(ownerUserIdOrNull, pageDetails);
  }

  private Page<Trip> pagedTripsForOwner(
      Long ownerUserId,
      boolean includePrivate,
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize) {
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    if (includePrivate) {
      return tripRepo.findByOwner_UserId(ownerUserId, pageDetails);
    }
    return tripRepo.findByOwner_UserIdAndIsPublicTrue(ownerUserId, pageDetails);
  }

  private TripListResponce toTripListResponse(Page<Trip> tripPage) {
    List<TripResponce> tripResponses = tripPage.getContent().stream().map(this::toResponse).toList();
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
  public TripResponce saveTrip(TriRequest triRequest, Long ownerUserId) {

    Trip trip = modelMapper.map(triRequest, Trip.class);
    trip.setCategories(
        triRequest.getCategories().stream().map(String::trim).distinct().toList());
    if (trip.getIsPublic() == null) {
      trip.setIsPublic(true);
    }

    String geocodeAddress;
    if (triRequest.getCityIds() != null
        && triRequest.getCityIds().stream().anyMatch(Objects::nonNull)) {
      applyCityIds(trip, triRequest.getCityIds());
      List<Long> nonNullIds =
          triRequest.getCityIds().stream().filter(Objects::nonNull).toList();
      Map<Long, City> byId =
          trip.getCities().stream().collect(Collectors.toMap(City::getId, c -> c));
      City primary = byId.get(nonNullIds.get(0));
      geocodeAddress = buildGeocodeAddressFromCity(primary);
    } else {
      geocodeAddress = buildGeocodeAddress(triRequest);
    }
    fillItineraryFromNearbySearch(trip, geocodeAddress);

    trip.setTitle(truncateTitle(generateTripTitle(trip, triRequest)));

    if (ownerUserId != null) {
      User owner = userRepository.findById(ownerUserId).orElse(null);
      if (owner != null) {
        trip.setOwner(owner);
      }
    }

    tripRepo.save(trip);

    return toResponse(trip);
  }

  @Override
  @Transactional
  public TripResponce deleteTrip(Long tripId, Long currentUserId) {
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    assertTripAccess(trip, currentUserId);
    TripResponce tripResponce = toResponse(trip);
    tripRepo.deleteById(tripId);
    return tripResponce;
  }

  @Override
  @Transactional
  public TripResponce updateTrip(Long tripId, TripUpdateRequest request, Long currentUserId) {
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    assertTripAccess(trip, currentUserId);

    if (request.getTitle() != null && !request.getTitle().isBlank()) {
      trip.setTitle(truncateTitle(request.getTitle().trim()));
    }
    if (request.getDesc() != null) {
      trip.setDesc(request.getDesc());
    }
    if (request.getBudget() != null) {
      trip.setBudget(request.getBudget());
    }
    if (request.getIsPublic() != null) {
      trip.setIsPublic(request.getIsPublic());
    }
    if (request.getStartDate() != null) {
      trip.setStartDate(request.getStartDate());
    }
    if (request.getEndDate() != null) {
      trip.setEndDate(request.getEndDate());
    }
    if (trip.getStartDate() != null
        && trip.getEndDate() != null
        && trip.getEndDate().isBefore(trip.getStartDate())) {
      throw new APIException("endDate must be on or after startDate");
    }

    if (request.getCityIds() != null) {
      applyCityIds(trip, request.getCityIds());
    }
    if (request.getCategories() != null) {
      if (request.getCategories().isEmpty()) {
        throw new APIException("categories cannot be empty when provided");
      }
      trip.setCategories(request.getCategories().stream().map(String::trim).distinct().toList());
    }

    boolean regenerate = Boolean.TRUE.equals(request.getRegenerateItinerary());
    if (regenerate) {
      if (trip.getCategories() == null || trip.getCategories().isEmpty()) {
        throw new APIException("categories are required to regenerate the itinerary");
      }
      String geocodeAddress = resolveGeocodeAddress(trip);
      trip.getDays().clear();
      fillItineraryFromNearbySearch(trip, geocodeAddress);
      boolean userSetTitle = request.getTitle() != null && !request.getTitle().isBlank();
      if (!userSetTitle) {
        trip.setTitle(truncateTitle(generateTripTitle(trip, null)));
      }
    }

    Trip savedTrip = tripRepo.save(trip);
    return toResponse(savedTrip);
  }

  @Override
  public TripResponce getTripById(Long tripId, Long userId) {
    Trip tripFromDb =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    TripResponce tripResponce = toResponse(tripFromDb);
    ratingService.attachRatingSummaries(tripResponce);
    attachUserActivityPreferences(tripResponce, userId);
    return tripResponce;
  }

  @Override
  @Transactional
  public TripResponce reorderDayActivities(
      Long tripId, Integer dayId, List<Long> orderedActivityIds) {
    Day day =
        dayRepository
            .findByIdAndTrip_Id(dayId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Day", "dayId", dayId.longValue()));

    Set<Long> expected =
        day.getActivities().stream().map(Activity::getId).collect(Collectors.toSet());
    if (orderedActivityIds.size() != expected.size()
        || !new HashSet<>(orderedActivityIds).equals(expected)) {
      throw new APIException(
          "orderedActivityIds must list every activity for this day exactly once, in the desired order");
    }

    List<Activity> toSave = new ArrayList<>();
    for (int i = 0; i < orderedActivityIds.size(); i++) {
      Long aid = orderedActivityIds.get(i);
      Activity activity =
          activityRepository
              .findById(aid)
              .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", aid));
      if (activity.getDay() == null || !activity.getDay().getId().equals(dayId)) {
        throw new APIException("Activity does not belong to this day");
      }
      activity.setSortOrder(i);
      toSave.add(activity);
    }
    activityRepository.saveAll(toSave);

    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    TripResponce tripResponce = toResponse(trip);
    ratingService.attachRatingSummaries(tripResponce);
    return tripResponce;
  }

  @Override
  @Transactional
  public TripResponce replaceActivityWithMockPlace(
      Long tripId, Long activityId, ReplaceActivityRequest request) {
    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", activityId));
    if (activity.getDay() == null
        || activity.getDay().getTrip() == null
        || !activity.getDay().getTrip().getId().equals(tripId)) {
      throw new APIException("Activity does not belong to this trip");
    }

    User user =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "userId", request.getUserId()));

    Page<Place> placePage = placeRepo.findAll(PageRequest.of(0, 1));
    if (placePage.isEmpty()) {
      throw new APIException(
          "No places in the database to use as a replacement (mock will be replaced later)");
    }
    Place mockPlace = placePage.getContent().get(0);

    UserActivityPreference pref =
        userActivityPreferenceRepository
            .findByUser_UserIdAndActivity_Id(request.getUserId(), activityId)
            .orElseGet(
                () -> {
                  UserActivityPreference p = new UserActivityPreference();
                  p.setUser(user);
                  p.setActivity(activity);
                  return p;
                });
    pref.setChangeReason(request.getReason());
    pref.setReplacementPlace(mockPlace);
    userActivityPreferenceRepository.save(pref);

    return getTripById(tripId, request.getUserId());
  }

  private TripResponce toResponse(Trip trip) {
    TripResponce r = modelMapper.map(trip, TripResponce.class);
    if (trip.getOwner() != null) {
      r.setOwnerId(trip.getOwner().getUserId());
    }
    return r;
  }

  private void assertTripAccess(Trip trip, Long currentUserId) {
    if (trip.getOwner() == null) {
      return;
    }
    if (currentUserId == null || !trip.getOwner().getUserId().equals(currentUserId)) {
      throw new APIException("Not allowed to modify this trip");
    }
  }

  private void applyCityIds(Trip trip, List<Long> cityIds) {
    if (cityIds == null || cityIds.stream().noneMatch(Objects::nonNull)) {
      trip.setCities(new HashSet<>());
      return;
    }
    List<Long> nonNullIds = cityIds.stream().filter(Objects::nonNull).toList();
    Set<Long> uniqueIds = new HashSet<>(nonNullIds);
    List<City> loaded = cityRepository.findAllByIdInWithCountry(uniqueIds);
    if (loaded.size() != uniqueIds.size()) {
      throw new APIException("One or more cities not found");
    }
    trip.setCities(new HashSet<>(loaded));
  }

  private String resolveGeocodeAddress(Trip trip) {
    if (trip.getCities() == null || trip.getCities().isEmpty()) {
      throw new APIException(
          "Trip has no cities; set cityIds before regenerating the itinerary");
    }
    City primary =
        trip.getCities().stream()
            .min(Comparator.comparing(City::getId))
            .orElseThrow();
    return buildGeocodeAddressFromCity(primary);
  }

  private void fillItineraryFromNearbySearch(Trip trip, String geocodeAddress) {
    LatLng center = googleGeocodingService.geocodeToLatLng(geocodeAddress);
    double radius = 10000.0;

    List<String> searchTypes = new ArrayList<>(new LinkedHashSet<>(trip.getCategories()));
    if (searchTypes.isEmpty()) {
      return;
    }

    List<Place> generatedPlaces =
        googlePlaceService.searchNearby(
            center.latitude(), center.longitude(), radius, searchTypes);
    List<Place> recommendedPlaces = placeRecommendationService.rankPlaces(generatedPlaces, searchTypes);

    if (!recommendedPlaces.isEmpty()) {
      List<Place> savedPlaces = new ArrayList<>();
      for (Place place : recommendedPlaces) {
        savedPlaces.add(placeRepo.save(place));
      }

      int placeIndex = 0;
      for (LocalDate d = trip.getStartDate(); !d.isAfter(trip.getEndDate()); d = d.plusDays(1)) {
        Day day = new Day();
        day.setDate(d);
        day.setTrip(trip);

        for (int i = 0; i < ACTIVITIES_PER_DAY; i++) {
          Place place = savedPlaces.get(placeIndex % savedPlaces.size());
          placeIndex++;

          Activity activity = new Activity();
          activity.setSortOrder(i);
          activity.setDay(day);
          activity.setPlaces(List.of(place));
          day.getActivities().add(activity);
        }

        trip.getDays().add(day);
      }
    }
  }

  private String generateTripTitle(Trip trip, TriRequest triRequest) {
    String place = primaryPlaceLabel(trip, triRequest);
    LocalDate start = trip.getStartDate();
    LocalDate end = trip.getEndDate();
    if (start == null) {
      return place;
    }
    if (end == null) {
      end = start;
    }
    DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);
    String startMonth = start.format(monthFmt);
    if (start.equals(end)) {
      return place + " in " + startMonth + " " + start.getYear();
    }
    if (start.getMonthValue() == end.getMonthValue() && start.getYear() == end.getYear()) {
      return place + " in " + startMonth + " " + start.getYear();
    }
    String endMonth = end.format(monthFmt);
    if (start.getYear() == end.getYear()) {
      return place + " in " + startMonth + "–" + endMonth + " " + start.getYear();
    }
    return place
        + " in "
        + startMonth
        + " "
        + start.getYear()
        + " – "
        + endMonth
        + " "
        + end.getYear();
  }

  /**
   * Place segment for titles, e.g. {@code Paris, France} or {@code Los Angeles, United States}.
   */
  private static String primaryPlaceLabel(Trip trip, TriRequest triRequest) {
    String requestCountry = requestCountryTrimmed(triRequest);
    if (trip.getCities() != null && !trip.getCities().isEmpty()) {
      City primary =
          trip.getCities().stream()
              .filter(c -> c.getName() != null && !c.getName().trim().isEmpty())
              .min(Comparator.comparing(c -> c.getName().trim(), String.CASE_INSENSITIVE_ORDER))
              .orElse(null);
      if (primary != null) {
        String cityName = primary.getName().trim();
        String countryName = countryNameFromCity(primary);
        if (countryName == null || countryName.isEmpty()) {
          countryName = requestCountry;
        }
        if (countryName != null && !countryName.isEmpty()) {
          return cityName + ", " + countryName;
        }
        return cityName;
      }
    }
    String city = requestCityTrimmed(triRequest);
    if (!city.isEmpty()) {
      if (!requestCountry.isEmpty()) {
        return city + ", " + requestCountry;
      }
      return city;
    }
    if (!requestCountry.isEmpty()) {
      return requestCountry;
    }
    return "Trip";
  }

  private static String countryNameFromCity(City city) {
    if (city.getCountry() == null
        || city.getCountry().getName() == null
        || city.getCountry().getName().isBlank()) {
      return "";
    }
    return city.getCountry().getName().trim();
  }

  private static String requestCityTrimmed(TriRequest triRequest) {
    if (triRequest == null || triRequest.getCity() == null) {
      return "";
    }
    String t = triRequest.getCity().trim();
    return t.isEmpty() ? "" : t;
  }

  private static String requestCountryTrimmed(TriRequest triRequest) {
    if (triRequest == null || triRequest.getCountry() == null) {
      return "";
    }
    String t = triRequest.getCountry().trim();
    return t.isEmpty() ? "" : t;
  }

  private static String truncateTitle(String raw) {
    if (raw == null || raw.isBlank()) {
      return "Trip";
    }
    String t = raw.trim();
    return t.length() <= 120 ? t : t.substring(0, 120);
  }

  private void attachUserActivityPreferences(TripResponce tripResponce, Long userId) {
    if (userId == null || tripResponce.getDays() == null) {
      return;
    }
    List<Long> activityIds = new ArrayList<>();
    for (DayResponse day : tripResponce.getDays()) {
      if (day.getActivities() == null) {
        continue;
      }
      for (ActivityResponse a : day.getActivities()) {
        if (a.getId() != null) {
          activityIds.add(a.getId());
        }
      }
    }
    if (activityIds.isEmpty()) {
      return;
    }
    List<UserActivityPreference> prefs =
        userActivityPreferenceRepository.findForUserAndActivities(userId, activityIds);
    Map<Long, UserActivityPreference> byActivityId =
        prefs.stream()
            .collect(Collectors.toMap(p -> p.getActivity().getId(), p -> p, (x, y) -> x));
    for (DayResponse day : tripResponce.getDays()) {
      if (day.getActivities() == null) {
        continue;
      }
      for (ActivityResponse a : day.getActivities()) {
        UserActivityPreference pref = byActivityId.get(a.getId());
        if (pref == null) {
          continue;
        }
        ActivityUserPreferenceResponse u = new ActivityUserPreferenceResponse();
        u.setReason(pref.getChangeReason());
        u.setReplacementPlaces(
            List.of(modelMapper.map(pref.getReplacementPlace(), PlaceResponse.class)));
        a.setUserPreference(u);
      }
    }
  }

  private static String buildGeocodeAddress(TriRequest triRequest) {
    String cityPart = triRequest.getCity() != null ? triRequest.getCity().trim() : "";
    String countryPart =
        triRequest.getCountry() != null ? triRequest.getCountry().trim() : "";
    if (!cityPart.isEmpty() && !countryPart.isEmpty()) {
      return cityPart + ", " + countryPart;
    }
    if (!countryPart.isEmpty()) {
      return countryPart;
    }
    return cityPart;
  }

  private static String buildGeocodeAddressFromCity(City city) {
    String cityPart = city.getName() != null ? city.getName().trim() : "";
    String countryPart =
        city.getCountry() != null && city.getCountry().getName() != null
            ? city.getCountry().getName().trim()
            : "";
    if (!cityPart.isEmpty() && !countryPart.isEmpty()) {
      return cityPart + ", " + countryPart;
    }
    if (!countryPart.isEmpty()) {
      return countryPart;
    }
    return cityPart;
  }
}
