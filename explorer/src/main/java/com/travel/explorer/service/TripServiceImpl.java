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
import com.travel.explorer.payload.trip.AddTripActivityRequest;
import com.travel.explorer.payload.trip.ReplaceActivitySmartRequest;
import com.travel.explorer.payload.trip.ReplaceActivityWithPlaceRequest;
import com.travel.explorer.payload.trip.TripOwnerResponse;
import com.travel.explorer.payload.trip.TriRequest;
import com.travel.explorer.payload.trip.TripListResponce;
import com.travel.explorer.payload.trip.TripResponce;
import com.travel.explorer.payload.trip.TripUpdateRequest;
import com.travel.explorer.repo.ActivityRepository;
import com.travel.explorer.repo.CityRepository;
import com.travel.explorer.repo.DayRepository;
import com.travel.explorer.repo.PlaceRepo;
import com.travel.explorer.repo.TripItineraryPlaceAdjustmentRepository;
import com.travel.explorer.repo.TripRatingRepository;
import com.travel.explorer.repo.TripRepo;
import com.travel.explorer.repo.TripSpecifications;
import com.travel.explorer.repo.UserActivityPreferenceRepository;
import com.travel.explorer.repo.UserRepository;
import com.travel.explorer.service.scheduling.HaversineUtil;
import com.travel.explorer.service.scheduling.PlaceGeoFilter;
import com.travel.explorer.service.scheduling.PlaceGeoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import java.util.stream.Stream;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;

@Service
public class TripServiceImpl implements TripService {

  private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

  private static final double TRIP_ITINERARY_SEARCH_RADIUS_METERS = 10_000;

  /** Max distance from trip center for any scheduled place (including saved must-include). */
  private static final double TRIP_MAX_PLACE_DISTANCE_METERS = TRIP_ITINERARY_SEARCH_RADIUS_METERS;

  private static final String SORT_AVERAGE_RATING = "averageRating";
  private static final String SORT_RATING_COUNT = "ratingCount";

  private static final RatingStats NO_RATINGS = new RatingStats(0.0, 0L);

  @Autowired
  private TripRepo tripRepo;

  @Autowired
  private TripRatingRepository tripRatingRepository;

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
  private PlacePhotoRefreshService placePhotoRefreshService;

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

  private static boolean isComputedTripSort(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return false;
    }
    return SORT_AVERAGE_RATING.equalsIgnoreCase(sortBy)
        || SORT_RATING_COUNT.equalsIgnoreCase(sortBy);
  }

  private Pageable buildTripPageable(
      String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
    if (isComputedTripSort(sortBy)) {
      return PageRequest.of(pageNumber, pageSize);
    }
    String entitySortField = mapTripSortField(sortBy);
    Sort sortByAndOrder =
        sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(entitySortField).ascending()
            : Sort.by(entitySortField).descending();
    return PageRequest.of(pageNumber, pageSize, sortByAndOrder);
  }

  /** Maps API sort keys to {@link Trip} property names. */
  private static String mapTripSortField(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "id";
    }
    return switch (sortBy) {
      case "startDate" -> "startDate";
      case "endDate" -> "endDate";
      case "title" -> "title";
      case "budget" -> "budget";
      case "isPublic" -> "isPublic";
      case "intensity", "tripIntensity" -> "intensity";
      default -> sortBy;
    };
  }

  private Page<Trip> pageGlobalTrips(
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize,
      List<String> categoryCodes,
      Long countryId,
      String countryName) {
    Specification<Trip> filter =
        TripSpecifications.fromFilters(categoryCodes, countryId, countryName);
    if (isComputedTripSort(sortBy)) {
      if (filter == null) {
        return pageGlobalTripsByRatingSort(sortBy, sortOrder, pageNumber, pageSize);
      }
      return pageTripsSortedByRating(filter, sortBy, sortOrder, pageNumber, pageSize);
    }
    Pageable pageable = buildTripPageable(sortBy, sortOrder, pageNumber, pageSize);
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
    Specification<Trip> base = TripSpecifications.ownedByUser(ownerUserId);
    if (!includePrivate) {
      base = base.and(TripSpecifications.isPublicTrip());
    }
    Specification<Trip> filter =
        TripSpecifications.fromFilters(categoryCodes, countryId, countryName);
    Specification<Trip> combined = filter != null ? base.and(filter) : base;
    if (isComputedTripSort(sortBy)) {
      return pageTripsSortedByRating(combined, sortBy, sortOrder, pageNumber, pageSize);
    }
    Pageable pageable = buildTripPageable(sortBy, sortOrder, pageNumber, pageSize);
    return tripRepo.findAll(combined, pageable);
  }

  private Page<Trip> pageGlobalTripsByRatingSort(
      String sortBy, String sortOrder, Integer pageNumber, Integer pageSize) {
    Pageable pageable = PageRequest.of(pageNumber, pageSize);
    boolean asc = sortOrder != null && sortOrder.equalsIgnoreCase("asc");
    Page<Long> idPage;
    if (SORT_RATING_COUNT.equalsIgnoreCase(sortBy)) {
      idPage =
          asc
              ? tripRepo.findAllTripIdsOrderByRatingCountAsc(pageable)
              : tripRepo.findAllTripIdsOrderByRatingCountDesc(pageable);
    } else {
      idPage =
          asc
              ? tripRepo.findAllTripIdsOrderByAverageRatingAsc(pageable)
              : tripRepo.findAllTripIdsOrderByAverageRatingDesc(pageable);
    }
    return tripsPageFromOrderedIds(idPage);
  }

  private Page<Trip> pageTripsSortedByRating(
      Specification<Trip> spec,
      String sortBy,
      String sortOrder,
      Integer pageNumber,
      Integer pageSize) {
    List<Trip> trips = tripRepo.findAll(spec);
    Map<Long, RatingStats> statsByTripId = loadRatingStatsByTripId();
    boolean asc = sortOrder != null && sortOrder.equalsIgnoreCase("asc");
    Comparator<Trip> comparator =
        SORT_RATING_COUNT.equalsIgnoreCase(sortBy)
            ? Comparator.comparingLong(
                t -> statsByTripId.getOrDefault(t.getId(), NO_RATINGS).count())
            : Comparator.comparingDouble(
                t -> statsByTripId.getOrDefault(t.getId(), NO_RATINGS).average());
    if (!asc) {
      comparator = comparator.reversed();
    }
    comparator = comparator.thenComparing(Trip::getId, Comparator.nullsLast(Long::compareTo));
    List<Trip> sorted = trips.stream().sorted(comparator).toList();
    Pageable pageable = PageRequest.of(pageNumber, pageSize);
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), sorted.size());
    List<Trip> pageContent = start >= sorted.size() ? List.of() : sorted.subList(start, end);
    return new PageImpl<>(pageContent, pageable, sorted.size());
  }

  private Map<Long, RatingStats> loadRatingStatsByTripId() {
    Map<Long, RatingStats> stats = new LinkedHashMap<>();
    for (Object[] row : tripRatingRepository.aggregateRatingStatsByTripId()) {
      Long tripId = (Long) row[0];
      Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
      long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
      stats.put(tripId, new RatingStats(avg, count));
    }
    return stats;
  }

  private Page<Trip> tripsPageFromOrderedIds(Page<Long> idPage) {
    List<Long> ids = idPage.getContent();
    if (ids.isEmpty()) {
      return Page.empty(idPage.getPageable());
    }
    Map<Long, Trip> tripsById =
        tripRepo.findAllWithOwnerByIdIn(ids).stream()
            .collect(Collectors.toMap(Trip::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
    List<Trip> ordered = ids.stream().map(tripsById::get).filter(Objects::nonNull).toList();
    return new PageImpl<>(ordered, idPage.getPageable(), idPage.getTotalElements());
  }

  private record RatingStats(double average, long count) {}

  private TripListResponce toTripListResponse(Page<Trip> tripPage) {
    List<Trip> trips = tripPage.getContent();
    if (trips.isEmpty()) {
      TripListResponce empty = new TripListResponce();
      empty.setContent(List.of());
      empty.setPageNumber(tripPage.getNumber());
      empty.setPageSize(tripPage.getSize());
      empty.setLastPage(tripPage.isLast());
      empty.setTotalPages(tripPage.getTotalPages());
      empty.setTotalElements(tripPage.getTotalElements());
      return empty;
    }

    List<Long> tripIds = trips.stream().map(Trip::getId).filter(Objects::nonNull).toList();
    Map<Long, Trip> tripsById =
        tripRepo.findAllWithOwnerByIdIn(tripIds).stream()
            .collect(Collectors.toMap(Trip::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
    List<Trip> orderedTrips =
        tripIds.stream().map(tripsById::get).filter(Objects::nonNull).toList();

    Map<Long, Long> coverPlaceIdByTripId = loadCoverPlaceIdByTripId(tripIds);

    List<TripResponce> tripResponses =
        orderedTrips.stream()
            .map(t -> toListSummaryResponse(t, coverPlaceIdByTripId.get(t.getId())))
            .filter(
                t ->
                    t.getCoverPhotoUrl() != null && !t.getCoverPhotoUrl().isBlank())
            .toList();
    ratingService.attachTripListRatingSummaries(tripResponses);

    TripListResponce tripListResponce = new TripListResponce();
    tripListResponce.setContent(tripResponses);
    tripListResponce.setPageNumber(tripPage.getNumber());
    tripListResponce.setPageSize(tripPage.getSize());
    tripListResponce.setLastPage(tripPage.isLast());
    tripListResponce.setTotalPages(tripPage.getTotalPages());
    tripListResponce.setTotalElements(tripPage.getTotalElements());
    return tripListResponce;
  }

  private Map<Long, Long> loadCoverPlaceIdByTripId(List<Long> tripIds) {
    if (tripIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> coverPlaces = new HashMap<>();
    for (Object[] row : tripRepo.findCoverPhotosByTripIds(tripIds)) {
      if (row[0] == null || row[2] == null) {
        continue;
      }
      Long tripId = ((Number) row[0]).longValue();
      Long placeId = ((Number) row[2]).longValue();
      coverPlaces.put(tripId, placeId);
    }
    return coverPlaces;
  }

  /** Lightweight card payload for paginated lists (no itinerary, no Google refresh). */
  private TripResponce toListSummaryResponse(Trip trip, Long coverPlaceId) {
    TripResponce r = new TripResponce();
    if (trip.getId() != null) {
      r.setId(trip.getId().intValue());
    }
    r.setTitle(trip.getTitle());
    r.setDesc(trip.getDesc());
    if (trip.getStartDate() != null) {
      r.setStartDate(trip.getStartDate().toString());
    }
    if (trip.getEndDate() != null) {
      r.setEndDate(trip.getEndDate().toString());
    }
    r.setCategories(trip.getCategories());
    r.setIntensity(trip.getIntensity());
    r.setIsPublic(trip.getIsPublic());
    applyListCoverPhoto(r, coverPlaceId);
    if (trip.getOwner() != null) {
      User o = trip.getOwner();
      r.setOwnerId(o.getUserId());
      r.setOwnerProfile(
          new TripOwnerResponse(o.getUserId(), o.getUsername(), o.getEmail(), o.getPhoneNumber()));
    }
    return r;
  }

  /**
   * List cards read cover image from {@code days[].activities[].places[].photoUrl} on the client;
   * include a minimal stub plus {@link TripResponce#setCoverPhotoUrl}.
   */
  private static void applyListCoverPhoto(TripResponce response, Long coverPlaceId) {
    String photoUrl = PlacePhotoService.publicPhotoUrl(coverPlaceId);
    response.setCoverPhotoUrl(photoUrl);
    if (coverPlaceId == null || photoUrl == null) {
      response.setDays(List.of());
      return;
    }
    PlaceResponse place = new PlaceResponse();
    place.setId(coverPlaceId);
    place.setPhotoUrl(photoUrl);
    ActivityResponse activity = new ActivityResponse();
    activity.setPlaces(List.of(place));
    DayResponse day = new DayResponse();
    day.setActivities(List.of(activity));
    response.setDays(List.of(day));
  }

  @Override
  @Transactional
  public TripResponce saveTrip(TriRequest triRequest, Long ownerUserId) {
    if (ownerUserId == null) {
      throw new APIException("Sign in is required to create a trip");
    }

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

    GeocodeTarget geocodeTarget;
    if (triRequest.getCityIds() != null
        && triRequest.getCityIds().stream().anyMatch(Objects::nonNull)) {
      applyCityIds(trip, triRequest.getCityIds());
      List<Long> nonNullIds =
          triRequest.getCityIds().stream().filter(Objects::nonNull).toList();
      Map<Long, City> byId =
          trip.getCities().stream().collect(Collectors.toMap(City::getId, c -> c));
      City primary = byId.get(nonNullIds.get(0));
      geocodeTarget = geocodeTargetFromCity(primary);
    } else {
      geocodeTarget = new GeocodeTarget(buildGeocodeAddress(triRequest), null);
    }
    fillItineraryFromNearbySearch(
        trip, geocodeTarget, ownerUserId, triRequest.getMustIncludePlaceIds());

    trip.setTitle(truncateTitle(generateTripTitle(trip, triRequest)));

    User owner = userRepository.findById(ownerUserId).orElse(null);
    if (owner == null) {
      throw new APIException("User not found");
    }
    trip.setOwner(owner);

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
      GeocodeTarget geocodeTarget = resolveGeocodeTarget(trip);
      trip.getDays().clear();
      fillItineraryFromNearbySearch(
          trip,
          geocodeTarget,
          trip.getOwner() != null ? trip.getOwner().getUserId() : null,
          null);
      boolean userSetTitle = request.getTitle() != null && !request.getTitle().isBlank();
      if (!userSetTitle) {
        trip.setTitle(truncateTitle(generateTripTitle(trip, null)));
      }
    }

    Trip savedTrip = tripRepo.save(trip);
    return toResponse(savedTrip);
  }

  @Override
  public TripResponce getTripById(Long tripId, Long userId, Long viewerUserIdOrNull) {
    Trip tripFromDb =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    assertTripViewable(tripFromDb, viewerUserIdOrNull);
    TripResponce tripResponce = toResponse(tripFromDb);
    ratingService.attachRatingSummaries(tripResponce);
    attachUserActivityPreferences(tripResponce, userId);
    return tripResponce;
  }

  @Override
  @Transactional
  public byte[] exportTripAsPdf(Long tripId, Long viewerUserIdOrNull) {
    Trip trip =
        tripRepo
            .findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "tripId", tripId));
    assertTripViewable(trip, viewerUserIdOrNull);
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
      Long tripId, Integer dayId, List<Long> orderedActivityIds, Long currentUserId) {
    Day day =
        dayRepository
            .findByIdAndTrip_Id(dayId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Day", "dayId", dayId.longValue()));
    assertTripAccess(day.getTrip(), currentUserId);

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

    return getTripById(tripId, currentUserId, currentUserId);
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
    return getTripById(tripId, currentUserId, currentUserId);
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
    return getTripById(tripId, currentUserId, currentUserId);
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

    return getTripById(tripId, currentUserId, currentUserId);
  }

  @Override
  @Transactional
  public TripResponce addTripActivity(
      Long tripId, Integer dayId, AddTripActivityRequest request, Long currentUserId) {
    Day day =
        dayRepository
            .findByIdAndTrip_Id(dayId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Day", "dayId", dayId.longValue()));
    Trip trip = day.getTrip();
    assertTripAccess(trip, currentUserId);

    Place place =
        placeRepo
            .findById(request.getPlaceId())
            .orElseThrow(() -> new ResourceNotFoundException("Place", "placeId", request.getPlaceId()));

    int nextOrder =
        day.getActivities().stream().mapToInt(Activity::getSortOrder).max().orElse(-1) + 1;

    Activity activity = new Activity();
    activity.setSortOrder(nextOrder);
    activity.setDay(day);
    activity.setUserAdded(true);
    activity.setPlaces(new ArrayList<>(List.of(place)));
    day.getActivities().add(activity);
    activityRepository.save(activity);

    removePlaceIdFromTripReserve(trip, place.getId());

    recordTripItineraryAdjustment(
        trip,
        currentUserId,
        ItineraryAdjustmentKind.ADD,
        ActivityChangeReason.ADDED,
        null,
        activity.getId(),
        null);

    return getTripById(tripId, currentUserId, currentUserId);
  }

  @Override
  @Transactional
  public TripResponce addTripActivityAuto(Long tripId, Integer dayId, Long currentUserId) {
    Day day =
        dayRepository
            .findByIdAndTrip_Id(dayId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Day", "dayId", dayId.longValue()));
    Trip trip = day.getTrip();
    assertTripAccess(trip, currentUserId);

    Long ownerUserId = trip.getOwner() != null ? trip.getOwner().getUserId() : null;
    Place chosen =
        pickAutoPlaceForTrip(trip, ownerUserId)
            .orElseThrow(() -> new APIException("No suitable place found to add for this trip"));

    int nextOrder =
        day.getActivities().stream().mapToInt(Activity::getSortOrder).max().orElse(-1) + 1;

    Activity activity = new Activity();
    activity.setSortOrder(nextOrder);
    activity.setDay(day);
    activity.setUserAdded(true);
    activity.setPlaces(new ArrayList<>(List.of(chosen)));
    day.getActivities().add(activity);
    activityRepository.save(activity);

    removePlaceIdFromTripReserve(trip, chosen.getId());

    recordTripItineraryAdjustment(
        trip,
        currentUserId,
        ItineraryAdjustmentKind.ADD,
        ActivityChangeReason.ADDED,
        null,
        activity.getId(),
        null);

    return getTripById(tripId, currentUserId, currentUserId);
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
    placePhotoRefreshService.refreshMissingPlacePhotos(allPlaces);

    TripResponce r = modelMapper.map(trip, TripResponce.class);
    if (trip.getCities() != null && !trip.getCities().isEmpty()) {
      List<Long> cityIds = new ArrayList<>();
      LinkedHashSet<Long> countryIds = new LinkedHashSet<>();
      for (City c : trip.getCities()) {
        if (c.getId() != null) {
          cityIds.add(c.getId());
        }
        if (c.getCountry() != null && c.getCountry().getId() != null) {
          countryIds.add(c.getCountry().getId());
        }
      }
      r.setCityIds(cityIds);
      r.setCountryIds(new ArrayList<>(countryIds));
    }
    if (trip.getOwner() != null) {
      User o = trip.getOwner();
      r.setOwnerId(o.getUserId());
      r.setOwnerProfile(
          new TripOwnerResponse(o.getUserId(), o.getUsername(), o.getEmail(), o.getPhoneNumber()));
    } else {
      r.setOwnerId(null);
      r.setOwnerProfile(null);
    }
    if (!allPlaces.isEmpty() && trip.getStartDate() != null && trip.getEndDate() != null) {
      int tripDays = (int) (trip.getEndDate().toEpochDay() - trip.getStartDate().toEpochDay()) + 1;
      r.setEstimatedBudget(budgetService.computeEstimatedBudget(allPlaces, tripDays));
    }
    rewritePlacePhotoUrlsToProxy(r.getDays());
    r.setCoverPhotoUrl(firstCoverPhotoUrl(r.getDays()));
    return r;
  }

  private static void rewritePlacePhotoUrlsToProxy(List<DayResponse> days) {
    if (days == null) {
      return;
    }
    for (DayResponse day : days) {
      if (day.getActivities() == null) {
        continue;
      }
      for (ActivityResponse activity : day.getActivities()) {
        if (activity.getPlaces() == null) {
          continue;
        }
        for (PlaceResponse place : activity.getPlaces()) {
          if (place != null && place.getId() != null) {
            place.setPhotoUrl(PlacePhotoService.publicPhotoUrl(place.getId()));
          }
        }
      }
    }
  }

  private static String firstCoverPhotoUrl(List<DayResponse> days) {
    if (days == null || days.isEmpty()) {
      return null;
    }
    return days.stream()
        .sorted(Comparator.comparing(DayResponse::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .flatMap(d -> d.getActivities() == null ? Stream.empty() : d.getActivities().stream())
        .flatMap(a -> a.getPlaces() == null ? Stream.empty() : a.getPlaces().stream())
        .map(PlaceResponse::getPhotoUrl)
        .filter(Objects::nonNull)
        .filter(url -> !url.isBlank())
        .findFirst()
        .orElse(null);
  }

  private void assertTripAccess(Trip trip, Long currentUserId) {
    if (trip.getOwner() == null) {
      return;
    }
    if (currentUserId == null || !trip.getOwner().getUserId().equals(currentUserId)) {
      throw new APIException("Not allowed to modify this trip");
    }
  }

  /** Private trips are readable only by the owner; public trips are readable by anyone. */
  private void assertTripViewable(Trip trip, Long viewerUserIdOrNull) {
    if (trip.getIsPublic() == null || Boolean.TRUE.equals(trip.getIsPublic())) {
      return;
    }
    if (trip.getOwner() == null) {
      return;
    }
    if (viewerUserIdOrNull != null
        && trip.getOwner().getUserId().equals(viewerUserIdOrNull)) {
      return;
    }
    throw new APIException("Trip is private");
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

  private record GeocodeTarget(String address, String countryIso) {}

  private record TripPlaceSearchContext(GeocodeTarget geocode, LatLng center, double radiusMeters) {}

  private TripPlaceSearchContext tripPlaceSearchContext(Trip trip) {
    GeocodeTarget target = resolveGeocodeTarget(trip);
    LatLng center =
        googleGeocodingService.geocodeToLatLng(target.address(), target.countryIso());
    return new TripPlaceSearchContext(target, center, TRIP_ITINERARY_SEARCH_RADIUS_METERS);
  }

  private boolean isWithinTripSearchRadius(Place place, TripPlaceSearchContext ctx) {
    if (place == null || place.getLocation() == null) {
      return false;
    }
    return !PlaceGeoFilter.withinRadius(
            List.of(place),
            ctx.center().latitude(),
            ctx.center().longitude(),
            ctx.radiusMeters())
        .isEmpty();
  }

  private GeocodeTarget resolveGeocodeTarget(Trip trip) {
    if (trip.getCities() != null && !trip.getCities().isEmpty()) {
      City primary =
          trip.getCities().stream()
              .min(Comparator.comparing(City::getId))
              .orElseThrow();
      return geocodeTargetFromCity(primary);
    }
    String fromPlaces = firstPlaceAddressOnTrip(trip);
    if (fromPlaces != null && !fromPlaces.isBlank()) {
      return new GeocodeTarget(fromPlaces.trim(), null);
    }
    if (trip.getTitle() != null && !trip.getTitle().isBlank()) {
      return new GeocodeTarget(trip.getTitle().trim(), null);
    }
    throw new APIException(
        "Trip has no cities; set cityIds on the trip or ensure itinerary places have addresses.");
  }

  private static GeocodeTarget geocodeTargetFromCity(City city) {
    return new GeocodeTarget(
        buildGeocodeAddressFromCity(city),
        city.getCountry() != null ? city.getCountry().getIso() : null);
  }

  /**
   * Reuses an existing row by {@code googlePlaceId} when present, but copies coordinates from the
   * freshly geocoded candidate so stale worldwide coordinates are not attached to the trip.
   */
  private Place resolvePersistedPlaceForItinerary(Place candidate) {
    if (candidate.getGooglePlaceId() != null && !candidate.getGooglePlaceId().isBlank()) {
      Place existing =
          placeRepo.findByGooglePlaceId(candidate.getGooglePlaceId()).orElse(null);
      if (existing != null) {
        PlaceCoordinateSync.applyFreshLocation(existing, candidate);
        return placeRepo.save(existing);
      }
    }
    if (candidate.getId() != null) {
      return candidate;
    }
    return placeRepo.save(candidate);
  }

  private void pruneActivitiesOutsideTripRadius(
      List<Day> days, double centerLat, double centerLng, double radiusMeters) {
    if (days == null) {
      return;
    }
    for (Day day : days) {
      if (day.getActivities() == null) {
        continue;
      }
      day.getActivities()
          .removeIf(
              activity -> {
                if (activity.getPlaces() == null || activity.getPlaces().isEmpty()) {
                  return true;
                }
                Place place = activity.getPlaces().get(0);
                return place.getLocation() == null
                    || PlaceGeoFilter.withinRadius(
                            List.of(place), centerLat, centerLng, radiusMeters)
                        .isEmpty();
              });
      int order = 0;
      for (Activity activity : day.getActivities()) {
        activity.setSortOrder(order++);
      }
    }
  }

  private void fillItineraryFromNearbySearch(
      Trip trip, GeocodeTarget geocodeTarget, Long ownerUserId, List<Long> mustIncludePlaceIds) {
    LatLng center =
        googleGeocodingService.geocodeToLatLng(
            geocodeTarget.address(), geocodeTarget.countryIso());
    double searchRadius = TRIP_ITINERARY_SEARCH_RADIUS_METERS;
    double centerLat = center.latitude();
    double centerLng = center.longitude();
    log.info(
        "Trip itinerary search center: {} ({}), radius {}m",
        geocodeTarget.address(),
        geocodeTarget.countryIso() != null ? "country=" + geocodeTarget.countryIso() : "no country bias",
        Math.round(TRIP_ITINERARY_SEARCH_RADIUS_METERS));

    List<String> searchTypes = new ArrayList<>(new LinkedHashSet<>(trip.getCategories()));
    if (searchTypes.isEmpty()) {
      return;
    }

    // 1. Aggregate candidates from Google API + DB (geo-filtered inside aggregator)
    List<Place> candidates =
        placeCandidateAggregator.aggregateCandidates(
            centerLat, centerLng, searchRadius, searchTypes);

    // 1b. Must-include saved places: only if within TRIP_MAX_PLACE_DISTANCE_METERS of trip center
    Set<Long> boostedIds = new HashSet<>();
    if (mustIncludePlaceIds != null && !mustIncludePlaceIds.isEmpty()) {
      Set<Long> requested = new LinkedHashSet<>();
      for (Long id : mustIncludePlaceIds) {
        if (id != null) {
          requested.add(id);
        }
      }
      Set<Long> alreadyPresent = new HashSet<>();
      for (Place p : candidates) {
        if (p.getId() != null) {
          alreadyPresent.add(p.getId());
        }
      }
      for (Long id : requested) {
        if (alreadyPresent.contains(id)) {
          boostedIds.add(id);
        }
      }
      List<Long> toLoad = new ArrayList<>();
      for (Long id : requested) {
        if (!alreadyPresent.contains(id)) {
          toLoad.add(id);
        }
      }
      if (!toLoad.isEmpty()) {
        candidates = new ArrayList<>(candidates);
        for (Place p : placeRepo.findAllById(toLoad)) {
          if (p.getLocation() == null) {
            log.warn(
                "Skipping must-include place id={} — no coordinates stored",
                p.getId());
            continue;
          }
          double km =
              HaversineUtil.distanceKm(
                  centerLat, centerLng, p.getLocation().getLat(), p.getLocation().getLng());
          if (km * 1000 > TRIP_MAX_PLACE_DISTANCE_METERS) {
            log.warn(
                "Skipping must-include place id={} — {} km from trip center (max {} km)",
                p.getId(),
                Math.round(km),
                Math.round(TRIP_MAX_PLACE_DISTANCE_METERS / 1000));
            continue;
          }
          candidates.add(p);
          boostedIds.add(p.getId());
        }
      }
    }

    // 2. Score with hybrid recommender (content + SVD), boosting must-include ids
    List<Place> rankedPlaces =
        placeRecommendationService.rankPlaces(
            candidates, searchTypes, ownerUserId, null, boostedIds.isEmpty() ? null : boostedIds);

    rankedPlaces =
        PlaceGeoFilter.withinRadius(
            rankedPlaces, centerLat, centerLng, TRIP_MAX_PLACE_DISTANCE_METERS);

    if (rankedPlaces.isEmpty()) {
      return;
    }

    // 3. Merge with existing DB records or persist new places (always keep fresh coordinates)
    List<Place> savedPlaces = new ArrayList<>();
    for (Place place : rankedPlaces) {
      savedPlaces.add(resolvePersistedPlaceForItinerary(place));
    }

    savedPlaces =
        PlaceGeoFilter.withinRadius(
            savedPlaces, centerLat, centerLng, TRIP_MAX_PLACE_DISTANCE_METERS);
    if (savedPlaces.isEmpty()) {
      log.warn(
          "No places within {}m of {} after resolving persisted records",
          Math.round(TRIP_MAX_PLACE_DISTANCE_METERS),
          geocodeTarget.address());
      return;
    }

    // 4. Schedule using ItineraryScheduler (handles budget, time, open hours)
    ItineraryScheduler.ScheduleResult result =
        itineraryScheduler.schedule(
            trip,
            savedPlaces,
            budgetService,
            trip.getBudget(),
            centerLat,
            centerLng,
            TRIP_MAX_PLACE_DISTANCE_METERS);

    trip.getDays().addAll(result.days());
    pruneActivitiesOutsideTripRadius(
        trip.getDays(), centerLat, centerLng, TRIP_MAX_PLACE_DISTANCE_METERS);

    if (trip.getItineraryReservePlaceIds() == null) {
      trip.setItineraryReservePlaceIds(new ArrayList<>());
    } else {
      trip.getItineraryReservePlaceIds().clear();
    }
    for (Place p : savedPlaces) {
      if (p.getId() == null || result.usedPlaceIds().contains(p.getId())) {
        continue;
      }
      if (p.getLocation() != null
          && !PlaceGeoFilter.withinRadius(
                  List.of(p), centerLat, centerLng, TRIP_MAX_PLACE_DISTANCE_METERS)
              .isEmpty()) {
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
        GeocodeTarget gt = geocodeTargetFromCity(cities.get(0));
        LatLng c = googleGeocodingService.geocodeToLatLng(gt.address(), gt.countryIso());
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
        GeocodeTarget gt = geocodeTargetFromCity(city);
        points.add(googleGeocodingService.geocodeToLatLng(gt.address(), gt.countryIso()));
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

    TripPlaceSearchContext searchCtx = tripPlaceSearchContext(trip);
    List<Long> reserve = trip.getItineraryReservePlaceIds();
    if (reserve != null) {
      for (Long pid : reserve) {
        if (pid == null || used.contains(pid)) {
          continue;
        }
        Place p = placeRepo.findById(pid).orElse(null);
        if (p == null || !isWithinTripSearchRadius(p, searchCtx)) {
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
        if (p.isPresent() && isWithinTripSearchRadius(p.get(), searchCtx)) {
          return p;
        }
      }
    }

    double radius = searchCtx.radiusMeters();
    List<String> searchTypes = new ArrayList<>(new LinkedHashSet<>(trip.getCategories()));
    if (searchTypes.isEmpty()) {
      return Optional.empty();
    }
    List<Place> candidates =
        placeCandidateAggregator.aggregateCandidates(
            searchCtx.center().latitude(),
            searchCtx.center().longitude(),
            radius,
            searchTypes);

    LinkedHashMap<Long, Place> byId = new LinkedHashMap<>();
    for (Place c : candidates) {
      if (c.getId() != null) {
        byId.put(c.getId(), c);
      }
    }
    if (reserve != null) {
      for (Long pid : reserve) {
        if (pid != null && !byId.containsKey(pid)) {
          placeRepo
              .findById(pid)
              .filter(p -> isWithinTripSearchRadius(p, searchCtx))
              .ifPresent(p -> byId.put(p.getId(), p));
        }
      }
    }

    List<Place> merged =
        PlaceGeoFilter.withinRadius(
            new ArrayList<>(byId.values()),
            searchCtx.center().latitude(),
            searchCtx.center().longitude(),
            radius);
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

  /**
   * Picks a place for a new activity: reserve list first (unused), else aggregated + ranked
   * candidates (same radius/categories as smart replace, no category-focus bias).
   */
  private Optional<Place> pickAutoPlaceForTrip(Trip trip, Long ownerUserId) {
    Set<Long> used = collectUsedPlaceIdsExcept(trip, null);

    TripPlaceSearchContext searchCtx = tripPlaceSearchContext(trip);
    List<Long> reserve = trip.getItineraryReservePlaceIds();
    if (reserve != null) {
      for (Long pid : reserve) {
        if (pid == null || used.contains(pid)) {
          continue;
        }
        Optional<Place> p = placeRepo.findById(pid);
        if (p.isPresent() && isWithinTripSearchRadius(p.get(), searchCtx)) {
          return p;
        }
      }
    }

    double radius = searchCtx.radiusMeters();
    List<String> searchTypes = new ArrayList<>(new LinkedHashSet<>(trip.getCategories()));
    if (searchTypes.isEmpty()) {
      return Optional.empty();
    }
    List<Place> candidates =
        placeCandidateAggregator.aggregateCandidates(
            searchCtx.center().latitude(),
            searchCtx.center().longitude(),
            radius,
            searchTypes);

    LinkedHashMap<Long, Place> byId = new LinkedHashMap<>();
    for (Place c : candidates) {
      if (c.getId() != null) {
        byId.put(c.getId(), c);
      }
    }
    if (reserve != null) {
      for (Long pid : reserve) {
        if (pid != null && !byId.containsKey(pid)) {
          placeRepo
              .findById(pid)
              .filter(p -> isWithinTripSearchRadius(p, searchCtx))
              .ifPresent(p -> byId.put(p.getId(), p));
        }
      }
    }

    List<Place> merged =
        PlaceGeoFilter.withinRadius(
            new ArrayList<>(byId.values()),
            searchCtx.center().latitude(),
            searchCtx.center().longitude(),
            radius);
    List<Place> ranked =
        placeRecommendationService.rankPlaces(merged, searchTypes, ownerUserId, null);

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
