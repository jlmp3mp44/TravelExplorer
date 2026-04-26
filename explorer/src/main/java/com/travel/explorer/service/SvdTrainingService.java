package com.travel.explorer.service;

import com.travel.explorer.entities.ActivityRating;
import com.travel.explorer.entities.Place;
import com.travel.explorer.repo.ActivityRatingRepository;
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

    private final ActivityRatingRepository activityRatingRepository;
    private final CollaborativeFilteringClient cfClient;

    public SvdTrainingService(ActivityRatingRepository activityRatingRepository,
                               CollaborativeFilteringClient cfClient) {
        this.activityRatingRepository = activityRatingRepository;
        this.cfClient = cfClient;
    }

    /**
     * Export all activity ratings as (userId, placeId, rating) triples
     * and send them to the Python SVD service for retraining.
     *
     * Each ActivityRating maps to one or more places via activity.places.
     * The same star rating is assigned to each place in the activity.
     */
    @Transactional(readOnly = true)
    public void exportAndRetrain() {
        List<ActivityRating> ratings = activityRatingRepository.findAllWithPlaces();
        if (ratings.isEmpty()) {
            log.info("No ratings to export for SVD training");
            return;
        }

        List<Map<String, Object>> ratingsData = new ArrayList<>();
        for (ActivityRating ar : ratings) {
            Long userId = ar.getUser().getUserId();
            int stars = ar.getStars();
            if (ar.getActivity().getPlaces() != null) {
                for (Place place : ar.getActivity().getPlaces()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("user_id", userId);
                    item.put("place_id", place.getId());
                    item.put("rating", (double) stars);
                    ratingsData.add(item);
                }
            }
        }

        // Deduplicate by (userId, placeId) — keep highest rating
        Map<String, Map<String, Object>> deduped = new HashMap<>();
        for (Map<String, Object> item : ratingsData) {
            String key = item.get("user_id") + "_" + item.get("place_id");
            Map<String, Object> existing = deduped.get(key);
            if (existing == null || (double) item.get("rating") > (double) existing.get("rating")) {
                deduped.put(key, item);
            }
        }
        List<Map<String, Object>> uniqueRatings = new ArrayList<>(deduped.values());

        log.info("Exporting {} unique user-place rating records for SVD training (before dedup: {})",
                uniqueRatings.size(), ratingsData.size());
        cfClient.triggerRetrain(uniqueRatings);
    }
}
