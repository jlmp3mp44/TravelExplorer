package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.google.GooglePlaceClient;
import com.travel.explorer.google.GooglePlacePhotoMediaUrlBuilder;
import com.travel.explorer.payload.place.GooglePlaceDto;
import com.travel.explorer.repo.PlaceRepo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Syncs {@link Place} metadata from Google: missing photos and non-English (e.g. Cyrillic) titles
 * are refreshed using {@code languageCode=en}.
 */
@Service
public class PlacePhotoRefreshService {

  private static final Logger log = LoggerFactory.getLogger(PlacePhotoRefreshService.class);

  private final PlaceRepo placeRepo;
  private final GooglePlaceClient googlePlaceClient;
  private final GooglePlacePhotoMediaUrlBuilder photoMediaUrlBuilder;
  private final Executor executor;
  private final int detailConcurrency;
  private final TransactionTemplate transactionTemplate;

  public PlacePhotoRefreshService(
      PlaceRepo placeRepo,
      GooglePlaceClient googlePlaceClient,
      GooglePlacePhotoMediaUrlBuilder photoMediaUrlBuilder,
      PlatformTransactionManager transactionManager,
      @Value("${google.search.detail-concurrency:5}") int detailConcurrency,
      @Value("${google.search.thread-pool-size:10}") int threadPoolSize) {
    this.placeRepo = placeRepo;
    this.googlePlaceClient = googlePlaceClient;
    this.photoMediaUrlBuilder = photoMediaUrlBuilder;
    this.detailConcurrency = detailConcurrency;
    this.executor = Executors.newFixedThreadPool(threadPoolSize);
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * For each distinct place with a Google id and no photo URL yet, loads details and persists a
   * media URL. Updates the given {@link Place} instances in memory when they represent the same id
   * so {@code ModelMapper} sees fresh values.
   */
  public void refreshMissingPlacePhotos(List<Place> places) {
    if (places == null || places.isEmpty()) {
      return;
    }
    Map<Long, Place> byId =
        places.stream()
            .filter(Objects::nonNull)
            .filter(p -> p.getId() != null)
            .collect(Collectors.toMap(Place::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

    List<Place> needs = new ArrayList<>();
    for (Place p : byId.values()) {
      if (p.getGooglePlaceId() == null || p.getGooglePlaceId().isBlank()) {
        continue;
      }
      boolean missingPhoto = p.getPhotoUrl() == null || p.getPhotoUrl().isBlank();
      boolean needsEnglishTitle = PlaceDisplaySync.titleNeedsEnglishRefresh(p.getTitle());
      if (!missingPhoto && !needsEnglishTitle) {
        continue;
      }
      needs.add(p);
    }
    if (needs.isEmpty()) {
      return;
    }

    Semaphore semaphore = new Semaphore(detailConcurrency);
    List<CompletableFuture<RefreshedPlaceFields>> futures =
        needs.stream()
            .map(
                p ->
                    CompletableFuture.supplyAsync(
                        () -> {
                          try {
                            semaphore.acquire();
                            try {
                              return placeRepo
                                  .findById(p.getId())
                                  .map(
                                      managed -> {
                                        boolean missingPhoto =
                                            managed.getPhotoUrl() == null
                                                || managed.getPhotoUrl().isBlank();
                                        boolean needsEnglishTitle =
                                            PlaceDisplaySync.titleNeedsEnglishRefresh(
                                                managed.getTitle());
                                        if (!missingPhoto && !needsEnglishTitle) {
                                          p.setPhotoUrl(managed.getPhotoUrl());
                                          if (managed.getTitle() != null) {
                                            p.setTitle(managed.getTitle());
                                          }
                                          return null;
                                        }
                                        if (managed.getGooglePlaceId() == null
                                            || managed.getGooglePlaceId().isBlank()) {
                                          return null;
                                        }
                                        try {
                                          GooglePlaceDto dto =
                                              googlePlaceClient.getPlaceDetails(
                                                  managed.getGooglePlaceId());
                                          PlaceDisplaySync.applyFreshDisplay(managed, dto);
                                          PlaceCoordinateSync.applyFreshLocation(managed, dto);
                                          if (managed.getTitle() != null) {
                                            p.setTitle(managed.getTitle());
                                          }
                                          if (managed.getLocation() != null) {
                                            p.setLocation(managed.getLocation());
                                          }
                                          String url = photoMediaUrlBuilder.firstPhotoMediaUrl(dto);
                                          if (url != null) {
                                            p.setPhotoUrl(url);
                                          }
                                          return new RefreshedPlaceFields(
                                              managed.getId(),
                                              missingPhoto ? url : null,
                                              needsEnglishTitle ? managed.getTitle() : null,
                                              needsEnglishTitle ? managed.getAddress() : null);
                                        } catch (Exception e) {
                                          log.warn(
                                              "Place metadata refresh failed for place {}: {}",
                                              managed.getId(),
                                              e.getMessage());
                                        }
                                        return null;
                                      })
                                  .orElse(null);
                            } finally {
                              semaphore.release();
                            }
                          } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("Photo refresh interrupted for place {}", p.getId());
                            return null;
                          }
                        },
                        executor))
            .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    Map<Long, RefreshedPlaceFields> toPersist = new LinkedHashMap<>();
    for (CompletableFuture<RefreshedPlaceFields> f : futures) {
      RefreshedPlaceFields e = f.join();
      if (e != null && e.placeId() != null) {
        toPersist.put(e.placeId(), e);
      }
    }
    if (!toPersist.isEmpty()) {
      transactionTemplate.executeWithoutResult(
          status -> {
            for (RefreshedPlaceFields e : toPersist.values()) {
              placeRepo
                  .findById(e.placeId())
                  .ifPresent(
                      managed -> {
                        if (e.photoUrl() != null && !e.photoUrl().isBlank()) {
                          managed.setPhotoUrl(e.photoUrl());
                        }
                        if (e.title() != null && !e.title().isBlank()) {
                          managed.setTitle(e.title());
                        }
                        if (e.address() != null && !e.address().isBlank()) {
                          managed.setAddress(e.address());
                        }
                        placeRepo.save(managed);
                      });
            }
          });
    }
  }

  private record RefreshedPlaceFields(
      Long placeId, String photoUrl, String title, String address) {}
}
