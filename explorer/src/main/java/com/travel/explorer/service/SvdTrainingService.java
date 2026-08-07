package com.travel.explorer.service;

import com.travel.explorer.entities.PlaceRating;
import com.travel.explorer.repo.PlaceRatingRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SvdTrainingService {

  private static final Logger log = LoggerFactory.getLogger(SvdTrainingService.class);

  private final PlaceRatingRepository placeRatingRepository;
  private final CollaborativeFilteringClient cfClient;

  public SvdTrainingService(
      PlaceRatingRepository placeRatingRepository, CollaborativeFilteringClient cfClient) {
    this.placeRatingRepository = placeRatingRepository;
    this.cfClient = cfClient;
  }

  /**
   * Export all place ratings as (userId, placeId, rating) triples and send them to the Python SVD
   * service for retraining. Ratings are global per place (shared across trips).
   */
  @Transactional(readOnly = true)
  public void exportAndRetrain() {
    List<PlaceRating> ratings = placeRatingRepository.findAllWithUserAndPlace();
    if (ratings.isEmpty()) {
      log.info("No ratings to export for SVD training");
      return;
    }

    List<Map<String, Object>> ratingsData = new ArrayList<>();
    for (PlaceRating pr : ratings) {
      Long userId = pr.getUser().getUserId();
      int stars = pr.getStars();
      if (pr.getPlace() == null || pr.getPlace().getId() == null) {
        continue;
      }
      Map<String, Object> item = new HashMap<>();
      item.put("user_id", userId);
      item.put("place_id", pr.getPlace().getId());
      item.put("rating", (double) stars);
      ratingsData.add(item);
    }

    Map<String, Map<String, Object>> deduped = new HashMap<>();
    for (Map<String, Object> item : ratingsData) {
      String key = item.get("user_id") + "_" + item.get("place_id");
      Map<String, Object> existing = deduped.get(key);
      if (existing == null || (double) item.get("rating") > (double) existing.get("rating")) {
        deduped.put(key, item);
      }
    }
    List<Map<String, Object>> uniqueRatings = new ArrayList<>(deduped.values());

    log.info(
        "Exporting {} unique user-place rating records for SVD training (before dedup: {})",
        uniqueRatings.size(),
        ratingsData.size());
    cfClient.triggerRetrain(uniqueRatings);
  }
}
