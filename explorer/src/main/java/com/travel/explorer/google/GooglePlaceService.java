package com.travel.explorer.google;

import com.travel.explorer.entities.Place;
import com.travel.explorer.service.PlacePhotoRefreshService;
import com.travel.explorer.google.request.Center;
import com.travel.explorer.google.request.Circle;
import com.travel.explorer.google.request.LocationBias;
import com.travel.explorer.google.request.LocationRestriction;
import com.travel.explorer.google.request.TextSearchRequest;
import com.travel.explorer.service.PlaceCoordinateSync;
import com.travel.explorer.service.PlaceDisplaySync;
import com.travel.explorer.service.scheduling.GeoBounds;
import com.travel.explorer.service.scheduling.HaversineUtil;
import com.travel.explorer.service.scheduling.PlaceGeoFilter;
import com.travel.explorer.google.response.GooglePlacesResponse;
import com.travel.explorer.payload.place.GooglePlaceDto;
import com.travel.explorer.repo.PlaceRepo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GooglePlaceService {

  private static final Logger log = LoggerFactory.getLogger(GooglePlaceService.class);

  private static final int PAGE_SIZE = 20;
  private static final int MAX_PAGES = 5;

  /**
   * {@code places:searchText} {@code locationBias.circle.radius} must be in [0, 50_000] meters
   * (Google Places API New).
   */
  private static final int TEXT_SEARCH_MAX_CIRCLE_RADIUS_METERS = 50_000;

  private static double clampTextSearchCircleRadiusMeters(double radiusMeters) {
    if (radiusMeters < 0) {
      return 0;
    }
    if (radiusMeters > TEXT_SEARCH_MAX_CIRCLE_RADIUS_METERS) {
      return TEXT_SEARCH_MAX_CIRCLE_RADIUS_METERS;
    }
    return radiusMeters;
  }

  private final GooglePlaceClient client;
  private final GooglePlaceMapper mapper;
  private final PlaceRepo placeRepo;
  private final PlacePhotoRefreshService placePhotoRefreshService;
  private final GooglePlacePhotoMediaUrlBuilder photoMediaUrlBuilder;
  private final Executor categoryExecutor;
  private final int detailConcurrency;

  public GooglePlaceService(
      GooglePlaceClient client,
      GooglePlaceMapper mapper,
      PlaceRepo placeRepo,
      PlacePhotoRefreshService placePhotoRefreshService,
      GooglePlacePhotoMediaUrlBuilder photoMediaUrlBuilder,
      @Value("${google.search.detail-concurrency:5}") int detailConcurrency,
      @Value("${google.search.thread-pool-size:10}") int threadPoolSize) {
    this.client = client;
    this.mapper = mapper;
    this.placeRepo = placeRepo;
    this.placePhotoRefreshService = placePhotoRefreshService;
    this.photoMediaUrlBuilder = photoMediaUrlBuilder;
    this.detailConcurrency = detailConcurrency;
    this.categoryExecutor = Executors.newFixedThreadPool(threadPoolSize);
  }

  /**
   * Searches for places across all given categories in parallel.
   * Uses the ID-first optimization: fetches only place IDs via the cheap Text Search SKU,
   * checks which IDs already exist in the DB, and only calls the expensive Place Details API
   * for unknown IDs.
   *
   * @param latitude       search center latitude
   * @param longitude      search center longitude
   * @param radius         search radius in meters
   * @param includedTypes  list of Google Place types (one API call per type, in parallel)
   * @return deduplicated list of Place entities (from DB cache + freshly fetched)
   */
  /**
   * Free-text search (user-typed name) restricted to a circle. Persists new places to the database.
   */
  public List<Place> searchByFreeText(
      String query, double latitude, double longitude, int radiusMeters) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    double r = clampTextSearchCircleRadiusMeters(radiusMeters);
    if (r != radiusMeters) {
      log.debug(
          "Text search circle radius clamped from {}m to {}m (API max {})",
          radiusMeters,
          r,
          TEXT_SEARCH_MAX_CIRCLE_RADIUS_METERS);
    }
    TextSearchRequest request =
        new TextSearchRequest(
            query.trim(),
            null,
            new LocationBias(new Circle(new Center(latitude, longitude), r)),
            null,
            GooglePlacesApi.LANGUAGE_CODE,
            PAGE_SIZE,
            null,
            null);

    GooglePlacesResponse response = client.searchTextFull(request);
    if (response == null || response.getPlaces() == null || response.getPlaces().isEmpty()) {
      return List.of();
    }

    List<Place> out = new ArrayList<>();
    for (GooglePlaceDto dto : response.getPlaces()) {
      if (dto.getGooglePlaceId() == null || dto.getGooglePlaceId().isBlank()) {
        continue;
      }
      Optional<Place> existing = placeRepo.findByGooglePlaceId(dto.getGooglePlaceId());
      if (existing.isPresent()) {
        Place ep = existing.get();
        PlaceCoordinateSync.applyFreshLocation(ep, dto);
        PlaceDisplaySync.applyFreshDisplay(ep, dto);
        if (ep.getPhotoUrl() == null || ep.getPhotoUrl().isBlank()) {
          String url = photoMediaUrlBuilder.firstPhotoMediaUrl(dto);
          if (url != null) {
            ep.setPhotoUrl(url);
          }
        }
        out.add(placeRepo.save(ep));
        continue;
      }
      try {
        Place place = mapper.toPlace(dto);
        out.add(placeRepo.save(place));
      } catch (Exception e) {
        log.warn(
            "Failed to persist place from text search {}: {}",
            dto.getGooglePlaceId(),
            e.getMessage());
      }
    }
    return out;
  }

  public List<Place> searchByCategories(
      double latitude, double longitude, double radius, List<String> includedTypes) {
    return searchByCategories(latitude, longitude, radius, includedTypes, true);
  }

  /**
   * @param restrictToSearchArea when true, uses {@code locationRestriction} (hard bounds);
   *     when false, uses {@code locationBias} only (results may lie outside the circle).
   */
  public List<Place> searchByCategories(
      double latitude,
      double longitude,
      double radius,
      List<String> includedTypes,
      boolean restrictToSearchArea) {

    if (includedTypes == null || includedTypes.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> distinct = includedTypes.stream().distinct().toList();

    // Phase 1: Collect all place IDs across all categories in parallel
    List<CompletableFuture<Set<String>>> idFutures = distinct.stream()
        .map(type -> CompletableFuture.supplyAsync(
            () -> collectPlaceIds(type, latitude, longitude, radius, restrictToSearchArea),
            categoryExecutor))
        .toList();

    CompletableFuture.allOf(idFutures.toArray(new CompletableFuture[0])).join();

    Set<String> allIds = new LinkedHashSet<>();
    for (CompletableFuture<Set<String>> f : idFutures) {
      allIds.addAll(f.join());
    }

    if (allIds.isEmpty()) {
      return Collections.emptyList();
    }

    log.info("Collected {} unique place IDs across {} categories", allIds.size(), distinct.size());

    // Phase 2: ID-first — partition into known (DB) and unknown
    List<Place> knownPlaces = placeRepo.findAllByGooglePlaceIdIn(allIds);
    Map<String, Place> knownById = knownPlaces.stream()
        .filter(p -> p.getGooglePlaceId() != null)
        .collect(Collectors.toMap(Place::getGooglePlaceId, Function.identity(),
            (a, b) -> a, LinkedHashMap::new));

    Set<String> unknownIds = new LinkedHashSet<>();
    for (String id : allIds) {
      if (!knownById.containsKey(id)) {
        unknownIds.add(id);
      }
    }

    if (restrictToSearchArea) {
      // Never trust cached coordinates for area-restricted search (trip itinerary).
      // Stale DB rows can sit inside the bounding box but belong to another region.
      List<String> staleKnown = new ArrayList<>();
      for (Place p : knownById.values()) {
        if (p.getGooglePlaceId() != null) {
          staleKnown.add(p.getGooglePlaceId());
          unknownIds.add(p.getGooglePlaceId());
        }
      }
      for (String id : staleKnown) {
        knownById.remove(id);
      }
      if (!staleKnown.isEmpty()) {
        log.info(
            "Re-fetching {} cached place(s) for fresh coordinates within {}m of search center",
            staleKnown.size(),
            Math.round(radius));
      }
    } else {
      List<String> staleKnown = new ArrayList<>();
      for (Place p : knownById.values()) {
        if (!isWithinRadiusMeters(p, latitude, longitude, radius)) {
          if (p.getGooglePlaceId() != null) {
            staleKnown.add(p.getGooglePlaceId());
            unknownIds.add(p.getGooglePlaceId());
          }
        }
      }
      for (String id : staleKnown) {
        knownById.remove(id);
      }
      if (!staleKnown.isEmpty()) {
        log.info(
            "Re-fetching {} cached place(s) with coordinates outside {}m of search center",
            staleKnown.size(),
            Math.round(radius));
      }
    }

    log.info("ID-first: {} known in DB, {} unknown — fetching details", knownById.size(), unknownIds.size());

    // Phase 3: Fetch full details for unknown IDs in parallel (rate-limited)
    List<Place> newlyFetched = fetchPlaceDetails(new ArrayList<>(unknownIds));

    // Persist newly fetched places (update coordinates on existing rows)
    List<Place> persistedFetched = new ArrayList<>(newlyFetched.size());
    for (Place place : newlyFetched) {
      try {
        persistedFetched.add(persistOrMergePlace(place));
      } catch (Exception e) {
        log.warn("Failed to persist place {}: {}", place.getGooglePlaceId(), e.getMessage());
      }
    }

    // Phase 4: Merge — freshly fetched details override stale DB copies
    Map<String, Place> result = new LinkedHashMap<>(knownById);
    for (Place p : persistedFetched) {
      if (p.getGooglePlaceId() != null) {
        result.put(p.getGooglePlaceId(), p);
      }
    }
    List<Place> out = new ArrayList<>(result.values());
    if (restrictToSearchArea) {
      out =
          PlaceGeoFilter.withinRadius(out, latitude, longitude, radius);
    }
    placePhotoRefreshService.refreshMissingPlacePhotos(out);
    return out;
  }

  private Place persistOrMergePlace(Place fresh) {
    if (fresh.getGooglePlaceId() != null && !fresh.getGooglePlaceId().isBlank()) {
      Optional<Place> existing = placeRepo.findByGooglePlaceId(fresh.getGooglePlaceId());
      if (existing.isPresent()) {
        Place merged = existing.get();
        PlaceCoordinateSync.applyFreshLocation(merged, fresh);
        PlaceDisplaySync.applyFreshDisplay(merged, fresh);
        return placeRepo.save(merged);
      }
    }
    return placeRepo.save(fresh);
  }

  private static boolean isWithinRadiusMeters(
      Place place, double centerLat, double centerLng, double radiusMeters) {
    if (place.getLocation() == null) {
      return false;
    }
    double km =
        HaversineUtil.distanceKm(
            centerLat,
            centerLng,
            place.getLocation().getLat(),
            place.getLocation().getLng());
    return km * 1000 <= radiusMeters;
  }

  /**
   * Collects all place IDs for a single type via paginated Text Search (ID-only field mask).
   */
  private Set<String> collectPlaceIds(
      String type, double lat, double lng, double radius, boolean restrictToSearchArea) {
    Set<String> ids = new LinkedHashSet<>();
    String pageToken = null;

    double clampedRadius = clampTextSearchCircleRadiusMeters(radius);
    LocationBias bias = null;
    LocationRestriction restriction = null;
    if (restrictToSearchArea) {
      restriction =
          LocationRestriction.rectangle(GeoBounds.boundingRectangle(lat, lng, clampedRadius));
    } else {
      bias = new LocationBias(new Circle(new Center(lat, lng), clampedRadius));
    }
    for (int page = 0; page < MAX_PAGES; page++) {
      TextSearchRequest request = new TextSearchRequest(
          type,
          type,
          bias,
          restriction,
          GooglePlacesApi.LANGUAGE_CODE,
          PAGE_SIZE,
          pageToken,
          true
      );

      try {
        GooglePlacesResponse response = client.searchTextIds(request);
        if (response == null || response.getPlaces() == null || response.getPlaces().isEmpty()) {
          break;
        }

        for (GooglePlaceDto dto : response.getPlaces()) {
          if (dto.getGooglePlaceId() != null && !dto.getGooglePlaceId().isBlank()) {
            ids.add(dto.getGooglePlaceId());
          }
        }

        pageToken = response.getNextPageToken();
        if (pageToken == null || pageToken.isBlank()) {
          break;
        }
      } catch (Exception e) {
        log.warn("Text search failed for type={} page={}: {}", type, page, e.getMessage());
        break;
      }
    }

    log.debug("Collected {} IDs for type={}", ids.size(), type);
    return ids;
  }

  /**
   * Fetches full place details for a list of place IDs, in parallel with rate limiting.
   */
  private List<Place> fetchPlaceDetails(List<String> placeIds) {
    if (placeIds.isEmpty()) {
      return Collections.emptyList();
    }

    Semaphore semaphore = new Semaphore(detailConcurrency);

    List<CompletableFuture<Place>> futures = placeIds.stream()
        .map(id -> CompletableFuture.supplyAsync(() -> {
          try {
            semaphore.acquire();
            try {
              GooglePlaceDto dto = client.getPlaceDetails(id);
              if (dto != null) {
                return mapper.toPlace(dto);
              }
            } finally {
              semaphore.release();
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Detail fetch interrupted for {}", id);
          } catch (Exception e) {
            log.warn("Detail fetch failed for {}: {}", id, e.getMessage());
          }
          return null;
        }, categoryExecutor))
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    return futures.stream()
        .map(CompletableFuture::join)
        .filter(p -> p != null)
        .toList();
  }

  /**
   * @deprecated Use {@link #searchByCategories} instead.
   */
  @Deprecated
  public List<Place> searchNearby(
      double latitude, double longitude, double radius, List<String> includedTypes) {
    return searchByCategories(latitude, longitude, radius, includedTypes);
  }
}