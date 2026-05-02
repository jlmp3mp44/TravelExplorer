package com.travel.explorer.service;

import com.travel.explorer.entities.Category;
import com.travel.explorer.entities.Place;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ContentBasedScorer {

    private static final double CATEGORY_MATCH_WEIGHT = 100.0;
    private static final double PRIMARY_TYPE_BONUS = 45.0;
    private static final double RATING_WEIGHT = 8.0;
    private static final double POPULARITY_WEIGHT = 8.0;
    private static final double OPERATIONAL_BONUS = 5.0;
    private static final double TEMPORARILY_CLOSED_PENALTY = 120.0;
    private static final double PERMANENTLY_CLOSED_PENALTY = 300.0;
    private static final double MISSING_TITLE_PENALTY = 15.0;
    private static final double MISSING_ADDRESS_PENALTY = 8.0;
    private static final double MISSING_CATEGORY_PENALTY = 20.0;

    /**
     * Compute a content-based score for a place given selected category codes.
     * Higher is better. Normalized to roughly 0-500 range.
     */
    public double score(Place place, Set<String> selectedCategories) {
        return scoreWithReplacementFocus(place, selectedCategories, null);
    }

    /**
     * Like {@link #score(Place, Set)} but adds extra weight for categories / primary type in
     * {@code replacementFocus} so replacements prefer the same kind of venue.
     */
    public double scoreWithReplacementFocus(
        Place place, Set<String> selectedCategories, Set<String> replacementFocus) {
        Set<String> placeCategories = extractCategoryCodes(place);
        double score = relevanceScore(place, placeCategories, selectedCategories);
        score += qualityScore(place);
        score += businessStatusScore(place);
        if (isBlank(place.getTitle())) score -= MISSING_TITLE_PENALTY;
        if (isBlank(place.getAddress())) score -= MISSING_ADDRESS_PENALTY;
        if (!selectedCategories.isEmpty() && placeCategories.isEmpty()) score -= MISSING_CATEGORY_PENALTY;
        if (replacementFocus != null && !replacementFocus.isEmpty()) {
            int boostHits = 0;
            for (String focus : replacementFocus) {
                if (focus == null) {
                    continue;
                }
                if (placeCategories.contains(focus)) {
                    boostHits++;
                }
                String prim = normalize(place.getPrimaryType());
                if (prim != null && prim.equals(focus)) {
                    boostHits++;
                }
            }
            score += boostHits * 85.0;
        }
        return score;
    }

    private double relevanceScore(Place place, Set<String> placeCategories, Set<String> selected) {
        if (selected.isEmpty()) {
            return 0.0;
        }
        int matchedCount = 0;
        for (String code : placeCategories) {
            if (selected.contains(code)) {
                matchedCount++;
            }
        }

        double score = matchedCount * CATEGORY_MATCH_WEIGHT;
        String primaryType = normalize(place.getPrimaryType());
        if (primaryType != null && selected.contains(primaryType)) {
            score += PRIMARY_TYPE_BONUS;
        }
        return score;
    }

    private double qualityScore(Place place) {
        double score = 0.0;

        if (place.getRating() != null) {
            double boundedRating = Math.max(0.0, Math.min(5.0, place.getRating()));
            score += boundedRating * RATING_WEIGHT;
        }
        if (place.getUserRatingCount() != null && place.getUserRatingCount() > 0) {
            score += Math.log10(place.getUserRatingCount() + 1.0) * POPULARITY_WEIGHT;
        }
        return score;
    }

    private double businessStatusScore(Place place) {
        if (Boolean.TRUE.equals(place.getPermanentlyClosed())) {
            return -PERMANENTLY_CLOSED_PENALTY;
        }
        if (Boolean.TRUE.equals(place.getTemporarilyClosed())) {
            return -TEMPORARILY_CLOSED_PENALTY;
        }

        String status = normalize(place.getBusinessStatus());
        if (Objects.equals(status, "operational")) {
            return OPERATIONAL_BONUS;
        }
        if (Objects.equals(status, "closed_temporarily")) {
            return -TEMPORARILY_CLOSED_PENALTY;
        }
        if (Objects.equals(status, "closed_permanently")) {
            return -PERMANENTLY_CLOSED_PENALTY;
        }
        return 0.0;
    }

    private Set<String> extractCategoryCodes(Place place) {
        Set<String> out = new HashSet<>();
        if (place.getCategories() == null) {
            return out;
        }
        for (Category category : place.getCategories()) {
            if (category == null) {
                continue;
            }
            String normalized = normalize(category.getName());
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return out;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
