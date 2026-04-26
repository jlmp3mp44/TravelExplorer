package com.travel.explorer.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CollaborativeFilteringClient {

    private static final Logger log = LoggerFactory.getLogger(CollaborativeFilteringClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CollaborativeFilteringClient(
            RestTemplateBuilder builder,
            @Value("${recommender.service.url:http://localhost:8001}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl;
    }

    /**
     * Get SVD-predicted ratings for the given user and place IDs.
     * Returns empty map on cold-start or if the service is unavailable (graceful fallback).
     */
    public Map<Long, Double> predictRatings(Long userId, List<Long> placeIds) {
        if (userId == null || placeIds == null || placeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("user_id", userId);
            request.put("place_ids", placeIds);

            ResponseEntity<PredictResponse> response = restTemplate.postForEntity(
                baseUrl + "/predict", request, PredictResponse.class);

            if (response.getBody() == null || response.getBody().getPredictions() == null) {
                return Collections.emptyMap();
            }

            // Convert String keys from JSON to Long keys
            Map<Long, Double> result = new HashMap<>();
            response.getBody().getPredictions().forEach((key, value) -> {
                try {
                    result.put(Long.parseLong(key), value);
                } catch (NumberFormatException e) {
                    log.warn("Invalid place id in SVD response: {}", key);
                }
            });
            return result;
        } catch (RestClientException e) {
            log.warn("SVD recommender service unavailable, falling back to content-based only: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Send ratings data to the Python service for model retraining.
     */
    public void triggerRetrain(List<Map<String, Object>> ratingsData) {
        if (ratingsData == null || ratingsData.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> request = Map.of("ratings", ratingsData);
            restTemplate.postForEntity(baseUrl + "/retrain", request, Void.class);
            log.info("SVD model retrain triggered with {} ratings", ratingsData.size());
        } catch (RestClientException e) {
            log.warn("Failed to trigger SVD retrain: {}", e.getMessage());
        }
    }

    // Inner DTO for deserializing the Python service response
    private static class PredictResponse {
        private Map<String, Double> predictions;

        public PredictResponse() {
        }

        public Map<String, Double> getPredictions() {
            return predictions;
        }

        public void setPredictions(Map<String, Double> predictions) {
            this.predictions = predictions;
        }
    }
}
