package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.ActivityChangeReason;
import com.travel.explorer.entities.Category;
import com.travel.explorer.entities.City;
import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.ItineraryAdjustmentKind;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.TripItineraryPlaceAdjustment;
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
import com.travel.explorer.payload.trip.ActivityManualEditRequest;
import com.travel.explorer.payload.trip.ReplaceActivitySmartRequest;
import com.travel.explorer.payload.trip.ReplaceActivityWithPlaceRequest;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.payload.trip.TripUpdateRequest;
import com.travel.explorer.repo.ActivityRepository;
import com.travel.explorer.repo.CityRepository;
import com.travel.explorer.repo.DayRepository;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripItineraryPlaceAdjustmentRepository;
import com.travel.explorer.repo.TripRepo;
import com.travel.explorer.repo.TripSpecifications;
import com.travel.explorer.repo.UserActivityPreferenceRepository;
import com.travel.explorer.repo.UserRepository;
import com.travel.explorer.service.scheduling.HaversineUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;

@Service
public class TripServiceImpl implements TripService {

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
  private TripItineraryPlaceAdjustmentRepository tripItineraryPlaceAdjustmentRepository;

  @Autowired
  ModelMapper modelMapper;

  @Autowired
  private GoogleGeocodingService googleGeocodingService;

  @Autowired
  private GooglePlaceService googlePlaceService;

  @Autowired
  private PlaceRecommendationService placeRecommendationService;

  @Autowired
  private PlaceCandidateAggregator placeCandidateAggregator;

  @Autowired
  private ItineraryScheduler itineraryScheduler;

  @Autowired
  private BudgetService budgetService;

  @Autowired
  private TripPdfExportService tripPdfExportService;

  @Override
  public TripListResponce getAllTrips(
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize,
      List<String> categoryCodes,
      Long countryId,
      String countryName) {
    Page<Trip> tripPage =
        pageGlobalTrips(
            sortBy, sortOrder, pageNumber, pageSize, categoryCodes, countryId, countryName);
    if (tripPage.getContent().isEmpty()) {
      boolean filtered =
          TripSpecifications.fromFilters(categoryCodes, countryId, countryName) != null;
      if (!filtered) {
        throw new APIException("No trips created till now");
      }
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
      Integer pageSize,
      List<String> categoryCodes,
      Long countryId,
      String countryName) {
    boolean ownerViewingSelf =
        viewerUserIdOrNull != null && viewerUserIdOrNull.equals(ownerUserId);
    Page<Trip> tripPage =
        pageOwnerTrips(
            ownerUserId,
            ownerViewingSelf,
            sortBy,
            sortOrder,
            pageNumber,
            pageSize,
            categoryCodes,
            countryId,
            countryName);
    return toTripListResponse(tripPage);
  }

  private Pageable buildTripPageable(
      String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    return PageRequest.of(pageNumber, pageSize, sortByAndOrder);
  }

  private Page<Trip> pageGlobalTrips(
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize,
      List<String> categoryCodes,
      Long countryId,
      String countryName) {
    Pageable pageable = buildTripPageable(sortBy, sortOrder, pageNumber, pageSize);
    Specification<Trip> filter =
        TripSpecifications.fromFilters(categoryCodes, countryId, countryName);
    if (filter == null) {
      return tripRepo.findAll(pageable);
    }
    return tripRepo.findAll(filter, pageable);
  }

  private Page<Trip> pageOwnerTrips(
      Long ownerUserId,
      boolean includePrivate,
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize,
      List<String> categoryCodes,
      Long countryId,
      String countryName) {
    Pageable pageable = buildTripPageable(sortBy, sortOrder, pageNumber, pageSize);
    Specification<Trip> base = TripSpecifications.ownedByUser(ownerUserId);
    if (!includePrivate) {
      base = base.and(TripSpecifications.isPublicTrip());
    }
    Specification<Trip> filter =
        TripSpecifications.fromFilters(categoryCodes, countryId, countryName);
    Specification<Trip> combined = filter != null ? base.and(filter) : base;
    return tripRepo.findAll(combined, pageable);
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
    trip.setIntensity(triRequest.getIntensity());
    if (trip.getIsPublic() == null) {
      trip.setIsPublic(true);
    }
    if (trip.getStartDate() != null && trip.getEndDate() != null
        && trip.getEndDate().isBefore(trip.getStartDate())) {
      throw new APIException("endDate must be on or after startDate");
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
    fillItineraryFromNearbySearch(trip, geocodeAddress, ownerUserId);

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
      fillItineraryFromNearbySearch(trip, geocodeAddress,
          trip.getOwner() != null ? trip.getOwner().getUserId() : null);
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
  public byte[] exportTripAsPdf(Long tripId) {
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    Hibernate.initialize(trip.getDays());
    for (Day d : trip.getDays()) {
      Hibernate.initialize(d.getActivities());
      for (Activity a : d.getActivities()) {
        Hibernate.initialize(a.getPlaces());
      }
    }
    return tripPdfExportService.buildTripPdf(trip);
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
  public List<PlaceResponse> searchTripPlaces(Long tripId, String query, Long currentUserId) {
    if (query == null || query.isBlank()) {
      throw new APIException("query is required");
    }
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    assertTripAccess(trip, currentUserId);
    TripSearchGeo geo = resolveTripSearchGeo(trip);
    List<Place> found =
        googlePlaceService.searchByFreeText(
            query.trim(), geo.center().latitude(), geo.center().longitude(), geo.radiusMeters());
    return found.stream().map(p -> modelMapper.map(p, PlaceResponse.class)).toList();
  }

  @Override
  @Transactional
  public TripResponce replaceActivitySmart(
      Long tripId, Long activityId, ReplaceActivitySmartRequest request, Long currentUserId) {
    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", activityId));
    if (activity.getDay() == null
        || activity.getDay().getTrip() == null
        || !activity.getDay().getTrip().getId().equals(tripId)) {
      throw new APIException("Activity does not belong to this trip");
    }
    Trip trip = activity.getDay().getTrip();
    assertTripAccess(trip, currentUserId);

    ActivityChangeReason reason =
        request != null && request.getReason() != null
            ? request.getReason()
            : ActivityChangeReason.DONT_WANT_TO_GO;

    Optional<Place> chosen =
        pickSmartReplacementPlace(activity, trip, trip.getOwner() != null ? trip.getOwner().getUserId() : null);
    if (chosen.isEmpty()) {
      throw new APIException("No suitable replacement place found for this activity");
    }
    applyActivityPlaceSwap(activity, trip, chosen.get(), currentUserId, reason);
    activityRepository.save(activity);
    tripRepo.save(trip);
    return getTripById(tripId, currentUserId);
  }

  @Override
  @Transactional
  public TripResponce replaceActivityWithPlace(
      Long tripId,
      Long activityId,
      ReplaceActivityWithPlaceRequest request,
      Long currentUserId) {
    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", activityId));
    if (activity.getDay() == null
        || activity.getDay().getTrip() == null
        || !activity.getDay().getTrip().getId().equals(tripId)) {
      throw new APIException("Activity does not belong to this trip");
    }
    Trip trip = activity.getDay().getTrip();
    assertTripAccess(trip, currentUserId);

    Place place =
        placeRepo
            .findById(request.getPlaceId())
            .orElseThrow(() -> new ResourceNotFoundException("Place", "placeId", request.getPlaceId()));

    ActivityChangeReason reason =
        request.getReason() != null ? request.getReason() : ActivityChangeReason.DONT_WANT_TO_GO;

    applyActivityPlaceSwap(activity, trip, place, currentUserId, reason);
    activityRepository.save(activity);
    tripRepo.save(trip);
    return getTripById(tripId, currentUserId);
  }

  @Override
  @Transactional
  public TripResponce deleteTripActivity(
      Long tripId,
      Long activityId,
      ActivityManualEditRequest request,
      Long currentUserId) {
    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity", "activityId", activityId));
    if (activity.getDay() == null
        || activity.getDay().getTrip() == null
        || !activity.getDay().getTrip().getId().equals(tripId)) {
      throw new APIException("Activity does not belong to this trip");
    }
    Trip trip = activity.getDay().getTrip();
    assertTripAccess(trip, currentUserId);

    Day day = activity.getDay();
    Long removedId = activity.getId();
    recordTripItineraryAdjustment(
        trip,
        currentUserId,
        ItineraryAdjustmentKind.REMOVE,
        request.getReason(),
        removedId,
        null,
        null);

    day.getActivities().remove(activity);
    activityRepository.delete(activity);

    List<Activity> remaining = new ArrayList<>(day.getActivities());
    remaining.sort(Comparator.comparing(Activity::getSortOrder));
    for (int i = 0; i < remaining.size(); i++) {
      remaining.get(i).setSortOrder(i);
    }
    activityRepository.saveAll(remaining);

    return getTripById(tripId, currentUserId);
  }

  @Override
  @Transactional
  public TripResponce addTripActivityWithMockPlace(Long tripId, Integer dayId, Long currentUserId) {
    Day day =
        dayRepository
            .findByIdAndTrip_Id(dayId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Day", "dayId", dayId.longValue()));
    Trip trip = day.getTrip();
    assertTripAccess(trip, currentUserId);

    Page<Place> placePage = placeRepo.findAll(PageRequest.of(0, 1));
    if (placePage.isEmpty()) {
      throw new APIException(
          "No places in the database to use as a replacement (mock will be replaced later)");
    }
    Place mockPlace = placePage.getContent().get(0);

    int nextOrder =
        day.getActivities().stream().mapToInt(Activity::getSortOrder).max().orElse(-1) + 1;

    Activity activity = new Activity();
    activity.setSortOrder(nextOrder);
    activity.setDay(day);
    activity.setUserAdded(true);
    activity.setPlaces(List.of(mockPlace));
    day.getActivities().add(activity);
    activityRepository.save(activity);

    return getTripById(tripId, currentUserId);
  }

  private void recordTripItineraryAdjustment(
      Trip trip,
      Long currentUserId,
      ItineraryAdjustmentKind kind,
      ActivityChangeReason reason,
      Long removedActivityId,
      Long createdActivityId,
      Long replacedActivityId) {
    TripItineraryPlaceAdjustment row = new TripItineraryPlaceAdjustment();
    row.setTrip(trip);
    User user = null;
    if (currentUserId != null) {
      user = userRepository.findById(currentUserId).orElse(null);
    }
    if (user == null) {
      user = trip.getOwner();
    }
    row.setUser(user);
    row.setKind(kind);
    row.setReason(reason);
    row.setRemovedActivityId(removedActivityId);
    row.setCreatedActivityId(createdActivityId);
    row.setReplacedActivityId(replacedActivityId);
    tripItineraryPlaceAdjustmentRepository.save(row);
  }

  private TripResponce toResponse(Trip trip) {
    TripResponce r = modelMapper.map(trip, TripResponce.class);
    if (trip.getOwner() != null) {
      r.setOwnerId(trip.getOwner().getUserId());
    }
    // Compute estimated budget from all places in the trip
    List<Place> allPlaces = new ArrayList<>();
    if (trip.getDays() != null) {
      for (Day day : trip.getDays()) {
        if (day.getActivities() != null) {
          for (Activity activity : day.getActivities()) {
            if (activity.getPlaces() != null) {
              allPlaces.addAll(activity.getPlaces());
            }
          }
        }
      }
    }
    if (!allPlaces.isEmpty() && trip.getStartDate() != null && trip.getEndDate() != null) {
      int tripDays = (int) (trip.getEndDate().toEpochDay() - trip.getStartDate().toEpochDay()) + 1;
      r.setEstimatedBudget(budgetService.computeEstimatedBudget(allPlaces, tripDays));
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
    if (trip.getCities() != null && !trip.getCities().isEmpty()) {
      City primary =
          trip.getCities().stream()
              .min(Comparator.comparing(City::getId))
              .orElseThrow();
      return buildGeocodeAddressFromCity(primary);
    }
    String fromPlaces = firstPlaceAddressOnTrip(trip);
    if (fromPlaces != null && !fromPlaces.isBlank()) {
      return fromPlaces.trim();
    }
    if (trip.getTitle() != null && !trip.getTitle().isBlank()) {
      return trip.getTitle().trim();
    }
    throw new APIException(
        "Trip has no cities; set cityIds on the trip or ensure itinerary places have addresses.");
  }

  private void fillItineraryFromNearbySearch(Trip trip, String geocodeAddress, Long ownerUserId) {
    LatLng center = googleGeocodingService.geocodeToLatLng(geocodeAddress);
    double radius = 10000.0;

    List<String> searchTypes = new ArrayList<>(new LinkedHashSet<>(trip.getCategories()));
    if (searchTypes.isEmpty()) {
      return;
    }

    // 1. Aggregate candidates from Google API + DB
    List<Place> candidates = placeCandidateAggregator.aggregateCandidates(
        center.latitude(), center.longitude(), radius, searchTypes);

    // 2. Score with hybrid recommender (content + SVD)
    List<Place> rankedPlaces = placeRecommendationService.rankPlaces(
        candidates, searchTypes, ownerUserId);

    if (rankedPlaces.isEmpty()) {
      return;
    }

    // 3. Merge with existing DB records or persist new places
    List<Place> savedPlaces = new ArrayList<>();
    for (Place place : rankedPlaces) {
      if (place.getId() != null) {
        // Already a persisted entity from DB aggregation
        savedPlaces.add(place);
      } else if (place.getGooglePlaceId() != null && !place.getGooglePlaceId().isBlank()) {
        // Try to find existing by googlePlaceId to avoid duplicates
        Place existing = placeRepo.findByGooglePlaceId(place.getGooglePlaceId()).orElse(null);
        if (existing != null) {
          savedPlaces.add(existing);
        } else {
          savedPlaces.add(placeRepo.save(place));
        }
      } else {
        savedPlaces.add(placeRepo.save(place));
      }
    }

    // 4. Schedule using ItineraryScheduler (handles budget, time, open hours)
    ItineraryScheduler.ScheduleResult result = itineraryScheduler.schedule(
        trip, savedPlaces, budgetService, trip.getBudget());

    trip.getDays().addAll(result.days());

    if (trip.getItineraryReservePlaceIds() == null) {
      trip.setItineraryReservePlaceIds(new ArrayList<>());
    } else {
      trip.getItineraryReservePlaceIds().clear();
    }
    for (Place p : savedPlaces) {
      if (p.getId() != null && !result.usedPlaceIds().contains(p.getId())) {
        trip.getItineraryReservePlaceIds().add(p.getId());
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

  private record TripSearchGeo(LatLng center, int radiusMeters) {}

  /**
   * Uses trip cities when present; otherwise derives center/radius from itinerary place coordinates
   * (trips created with free-text city/country often have no {@link Trip#getCities()} rows).
   */
  private TripSearchGeo resolveTripSearchGeo(Trip trip) {
    if (trip.getCities() != null && !trip.getCities().isEmpty()) {
      List<City> cities = new ArrayList<>(trip.getCities());
      if (cities.size() == 1) {
        LatLng c =
            googleGeocodingService.geocodeToLatLng(buildGeocodeAddressFromCity(cities.get(0)));
        return new TripSearchGeo(c, 40_000);
      }
      Set<Long> countryIds =
          cities.stream()
              .filter(ct -> ct.getCountry() != null && ct.getCountry().getId() != null)
              .map(ct -> ct.getCountry().getId())
              .collect(Collectors.toSet());
      if (countryIds.size() != 1) {
        throw new APIException(
            "Place search is only supported for trips in a single country when multiple cities are selected");
      }
      List<LatLng> points = new ArrayList<>();
      for (City city : cities) {
        points.add(googleGeocodingService.geocodeToLatLng(buildGeocodeAddressFromCity(city)));
      }
      return tripSearchGeoFromLatLngPoints(points);
    }
    List<LatLng> fromItinerary = collectLatLngsFromTripPlaces(trip);
    if (fromItinerary.isEmpty()) {
      throw new APIException(
          "Trip has no cities and no coordinates on itinerary places; set cityIds on the trip or add stops with map locations.");
    }
    return tripSearchGeoFromLatLngPoints(fromItinerary);
  }

  private static TripSearchGeo tripSearchGeoFromLatLngPoints(List<LatLng> points) {
    if (points.isEmpty()) {
      throw new IllegalArgumentException("points must not be empty");
    }
    if (points.size() == 1) {
      return new TripSearchGeo(points.get(0), 40_000);
    }
    double sumLat = points.stream().mapToDouble(LatLng::latitude).sum();
    double sumLng = points.stream().mapToDouble(LatLng::longitude).sum();
    LatLng center = new LatLng(sumLat / points.size(), sumLng / points.size());
    double maxKm = 0;
    for (LatLng p : points) {
      maxKm =
          Math.max(
              maxKm,
              HaversineUtil.distanceKm(
                  center.latitude(), center.longitude(), p.latitude(), p.longitude()));
    }
    int radiusMeters = (int) Math.min(500_000, Math.max(80_000, maxKm * 1000 * 1.4 + 50_000));
    return new TripSearchGeo(center, radiusMeters);
  }

  private static List<LatLng> collectLatLngsFromTripPlaces(Trip trip) {
    List<LatLng> out = new ArrayList<>();
    if (trip.getDays() == null) {
      return out;
    }
    for (Day d : trip.getDays()) {
      if (d.getActivities() == null) {
        continue;
      }
      for (Activity a : d.getActivities()) {
        if (a.getPlaces() == null) {
          continue;
        }
        for (Place p : a.getPlaces()) {
          if (p.getLocation() == null) {
            continue;
          }
          out.add(new LatLng(p.getLocation().getLat(), p.getLocation().getLng()));
        }
      }
    }
    return out;
  }

  private static String firstPlaceAddressOnTrip(Trip trip) {
    if (trip.getDays() == null) {
      return null;
    }
    for (Day d : trip.getDays()) {
      if (d.getActivities() == null) {
        continue;
      }
      for (Activity a : d.getActivities()) {
        if (a.getPlaces() == null) {
          continue;
        }
        for (Place p : a.getPlaces()) {
          if (p.getAddress() != null && !p.getAddress().isBlank()) {
            return p.getAddress();
          }
        }
      }
    }
    return null;
  }

  private Set<String> normalizedCategoriesFromPlace(Place place) {
    Set<String> out = new LinkedHashSet<>();
    if (place == null) {
      return out;
    }
    if (place.getPrimaryType() != null) {
      String n = normalizeCategoryToken(place.getPrimaryType());
      if (n != null) {
        out.add(n);
      }
    }
    if (place.getCategories() != null) {
      for (Category c : place.getCategories()) {
        if (c == null || c.getName() == null) {
          continue;
        }
        String n = normalizeCategoryToken(c.getName());
        if (n != null) {
          out.add(n);
        }
      }
    }
    return out;
  }

  private static String normalizeCategoryToken(String raw) {
    if (raw == null) {
      return null;
    }
    String t = raw.trim().toLowerCase(Locale.ROOT);
    return t.isEmpty() ? null : t;
  }

  private boolean matchesCategoryFocus(Place place, Set<String> focus) {
    if (focus == null || focus.isEmpty()) {
      return false;
    }
    Set<String> placeCats = normalizedCategoriesFromPlace(place);
    for (String f : focus) {
      if (placeCats.contains(f)) {
        return true;
      }
    }
    return false;
  }

  private Set<Long> collectUsedPlaceIdsExcept(Trip trip, Long excludeActivityId) {
    Set<Long> used = new HashSet<>();
    if (trip.getDays() == null) {
      return used;
    }
    for (Day d : trip.getDays()) {
      if (d.getActivities() == null) {
        continue;
      }
      for (Activity a : d.getActivities()) {
        if (excludeActivityId != null && excludeActivityId.equals(a.getId())) {
          continue;
        }
        if (a.getPlaces() == null) {
          continue;
        }
        for (Place p : a.getPlaces()) {
          if (p.getId() != null) {
            used.add(p.getId());
          }
        }
      }
    }
    return used;
  }

  private Optional<Place> pickSmartReplacementPlace(Activity activity, Trip trip, Long ownerUserId) {
    Place currentPlace =
        activity.getPlaces() == null || activity.getPlaces().isEmpty()
            ? null
            : activity.getPlaces().get(0);
    Set<String> focus =
        currentPlace == null ? Set.of() : normalizedCategoriesFromPlace(currentPlace);
    Set<Long> used = collectUsedPlaceIdsExcept(trip, activity.getId());

    List<Long> reserve = trip.getItineraryReservePlaceIds();
    if (reserve != null) {
      for (Long pid : reserve) {
        if (pid == null || used.contains(pid)) {
          continue;
        }
        Place p = placeRepo.findById(pid).orElse(null);
        if (p == null) {
          continue;
        }
        if (!focus.isEmpty() && matchesCategoryFocus(p, focus)) {
          return Optional.of(p);
        }
      }
      for (Long pid : reserve) {
        if (pid == null || used.contains(pid)) {
          continue;
        }
        Optional<Place> p = placeRepo.findById(pid);
        if (p.isPresent()) {
          return p;
        }
      }
    }

    String geocodeAddress = resolveGeocodeAddress(trip);
    LatLng center = googleGeocodingService.geocodeToLatLng(geocodeAddress);
    double radius = 10000.0;
    List<String> searchTypes = new ArrayList<>(new LinkedHashSet<>(trip.getCategories()));
    if (searchTypes.isEmpty()) {
      return Optional.empty();
    }
    List<Place> candidates =
        placeCandidateAggregator.aggregateCandidates(
            center.latitude(), center.longitude(), radius, searchTypes);

    LinkedHashMap<Long, Place> byId = new LinkedHashMap<>();
    for (Place c : candidates) {
      if (c.getId() != null) {
        byId.put(c.getId(), c);
      }
    }
    if (reserve != null) {
      for (Long pid : reserve) {
        if (pid != null && !byId.containsKey(pid)) {
          placeRepo.findById(pid).ifPresent(p -> byId.put(p.getId(), p));
        }
      }
    }

    List<Place> merged = new ArrayList<>(byId.values());
    List<Place> ranked =
        placeRecommendationService.rankPlaces(
            merged, searchTypes, ownerUserId, focus.isEmpty() ? null : focus);

    for (Place p : ranked) {
      if (p.getId() == null || used.contains(p.getId())) {
        continue;
      }
      if (!focus.isEmpty() && matchesCategoryFocus(p, focus)) {
        return placeRepo.findById(p.getId());
      }
    }
    for (Place p : ranked) {
      if (p.getId() != null && !used.contains(p.getId())) {
        return placeRepo.findById(p.getId());
      }
    }
    return Optional.empty();
  }

  private void removePlaceIdFromTripReserve(Trip trip, Long placeId) {
    if (placeId == null || trip.getItineraryReservePlaceIds() == null) {
      return;
    }
    trip.getItineraryReservePlaceIds().removeIf(id -> id != null && id.equals(placeId));
  }

  private void applyActivityPlaceSwap(
      Activity activity,
      Trip trip,
      Place newPlace,
      Long currentUserId,
      ActivityChangeReason reason) {
    Place oldPrimary =
        activity.getPlaces() == null || activity.getPlaces().isEmpty()
            ? null
            : activity.getPlaces().get(0);
    Long oldId = oldPrimary != null ? oldPrimary.getId() : null;

    if (activity.getPlaces() == null) {
      activity.setPlaces(new ArrayList<>());
    } else {
      activity.getPlaces().clear();
    }
    activity.getPlaces().add(newPlace);
    activity.setUserAdded(true);

    removePlaceIdFromTripReserve(trip, newPlace.getId());
    if (oldId != null) {
      if (trip.getItineraryReservePlaceIds() == null) {
        trip.setItineraryReservePlaceIds(new ArrayList<>());
      }
      if (!trip.getItineraryReservePlaceIds().contains(oldId)) {
        trip.getItineraryReservePlaceIds().add(oldId);
      }
    }

    recordTripItineraryAdjustment(
        trip,
        currentUserId,
        ItineraryAdjustmentKind.REPLACE,
        reason,
        null,
        null,
        activity.getId());
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
