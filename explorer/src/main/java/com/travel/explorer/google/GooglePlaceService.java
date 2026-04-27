package com.travel.explorer.google;

import com.travel.explorer.entities.Place;
import com.travel.explorer.google.request.Center;
import com.travel.explorer.google.request.Circle;
import com.travel.explorer.google.request.LocationRestriction;
import com.travel.explorer.google.request.TextSearchRequest;
import com.travel.explorer.google.response.GooglePlacesResponse;
import com.travel.explorer.payload.place.GooglePlaceDto;
import com.travel.explorer.repo.PlaceRepo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
  private static final String LANGUAGE_UK = "uk";

  private final GooglePlaceClient client;
  private final GooglePlaceMapper mapper;
  private final PlaceRepo placeRepo;
  private final Executor categoryExecutor;
  private final int detailConcurrency;

  public GooglePlaceService(
      GooglePlaceClient client,
      GooglePlaceMapper mapper,
      PlaceRepo placeRepo,
      @Value("${google.search.detail-concurrency:5}") int detailConcurrency,
      @Value("${google.search.thread-pool-size:10}") int threadPoolSize) {
    this.client = client;
    this.mapper = mapper;
    this.placeRepo = placeRepo;
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
  public List<Place> searchByCategories(
      double latitude, double longitude, double radius, List<String> includedTypes) {

    if (includedTypes == null || includedTypes.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> distinct = includedTypes.stream().distinct().toList();

    // Phase 1: Collect all place IDs across all categories in parallel
    List<CompletableFuture<Set<String>>> idFutures = distinct.stream()
        .map(type -> CompletableFuture.supplyAsync(
            () -> collectPlaceIds(type, latitude, longitude, radius),
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

    log.info("ID-first: {} known in DB, {} unknown — fetching details", knownById.size(), unknownIds.size());

    // Phase 3: Fetch full details for unknown IDs in parallel (rate-limited)
    List<Place> newlyFetched = fetchPlaceDetails(new ArrayList<>(unknownIds));

    // Persist newly fetched places
    for (Place place : newlyFetched) {
      try {
        placeRepo.save(place);
      } catch (Exception e) {
        log.warn("Failed to persist place {}: {}", place.getGooglePlaceId(), e.getMessage());
      }
    }

    // Phase 4: Merge
    Map<String, Place> result = new LinkedHashMap<>(knownById);
    for (Place p : newlyFetched) {
      if (p.getGooglePlaceId() != null) {
        result.putIfAbsent(p.getGooglePlaceId(), p);
      }
    }
    return new ArrayList<>(result.values());
  }

  /**
   * Collects all place IDs for a single type via paginated Text Search (ID-only field mask).
   */
  private Set<String> collectPlaceIds(String type, double lat, double lng, double radius) {
    Set<String> ids = new LinkedHashSet<>();
    String pageToken = null;

    for (int page = 0; page < MAX_PAGES; page++) {
      TextSearchRequest request = new TextSearchRequest(
          type,
          type,
          new LocationRestriction(new Circle(new Center(lat, lng), radius)),
          LANGUAGE_UK,
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