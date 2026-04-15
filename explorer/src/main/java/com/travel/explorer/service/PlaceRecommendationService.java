package com.travel.explorer.service;

import com.travel.explorer.entities.Category;
import com.travel.explorer.entities.Place;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PlaceRecommendationService {

  private static final double CATEGORY_MATCH_WEIGHT = 100.0;
  private static final double PRIMARY_TYPE_BONUS = 45.0;
  private static final double RATING_WEIGHT = 8.0;
  private static final double POPULARITY_WEIGHT = 8.0;
  private static final double DIVERSITY_PENALTY = 10.0;
  private static final double OPERATIONAL_BONUS = 5.0;
  private static final double TEMPORARILY_CLOSED_PENALTY = 120.0;
  private static final double PERMANENTLY_CLOSED_PENALTY = 300.0;
  private static final double MISSING_TITLE_PENALTY = 15.0;
  private static final double MISSING_ADDRESS_PENALTY = 8.0;
  private static final double MISSING_CATEGORY_PENALTY = 20.0;

  public List<Place> rankPlaces(List<Place> places, List<String> selectedCategoryCodes) {
    if (places == null || places.isEmpty()) {
      return List.of();
    }

    Set<String> selected = normalizeToSet(selectedCategoryCodes);
    List<RankedPlace> remaining = new ArrayList<>(places.size());
    for (Place place : places) {
      Set<String> placeCategories = extractCategoryCodes(place);
      remaining.add(new RankedPlace(place, placeCategories, baseScore(place, placeCategories, selected)));
    }
    remaining.sort(BASE_COMPARATOR);

    Map<String, Integer> categoryUsage = new HashMap<>();
    List<Place> ranked = new ArrayList<>(remaining.size());
    while (!remaining.isEmpty()) {
      RankedPlace next = chooseNext(remaining, categoryUsage);
      remaining.remove(next);
      next.place().setTotalScore((int) Math.round(adjustedScore(next, categoryUsage)));
      ranked.add(next.place());
      for (String code : next.matchedSelectedCategories(selected)) {
        categoryUsage.merge(code, 1, Integer::sum);
      }
    }
    return ranked;
  }

  private double baseScore(Place place, Set<String> placeCategories, Set<String> selected) {
    double score = relevanceScore(place, placeCategories, selected);
    score += qualityScore(place);
    score += businessStatusScore(place);

    if (isBlank(place.getTitle())) {
      score -= MISSING_TITLE_PENALTY;
    }
    if (isBlank(place.getAddress())) {
      score -= MISSING_ADDRESS_PENALTY;
    }
    if (!selected.isEmpty() && placeCategories.isEmpty()) {
      score -= MISSING_CATEGORY_PENALTY;
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

  private double adjustedScore(RankedPlace candidate, Map<String, Integer> categoryUsage) {
    int overlapUsage = 0;
    for (String code : candidate.matchedSelectedCategories(categoryUsage.keySet())) {
      overlapUsage += categoryUsage.getOrDefault(code, 0);
    }
    return candidate.baseScore() - (overlapUsage * DIVERSITY_PENALTY);
  }

  private RankedPlace chooseNext(List<RankedPlace> candidates, Map<String, Integer> categoryUsage) {
    RankedPlace best = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (RankedPlace candidate : candidates) {
      double adjusted = adjustedScore(candidate, categoryUsage);
      if (adjusted > bestScore) {
        bestScore = adjusted;
        best = candidate;
      } else if (adjusted == bestScore && best != null && BASE_COMPARATOR.compare(candidate, best) < 0) {
        best = candidate;
      }
    }
    return best != null ? best : candidates.get(0);
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

  private Set<String> normalizeToSet(List<String> values) {
    Set<String> out = new HashSet<>();
    if (values == null) {
      return out;
    }
    for (String value : values) {
      String normalized = normalize(value);
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

  private static final Comparator<RankedPlace> BASE_COMPARATOR =
      Comparator.comparingDouble(RankedPlace::baseScore)
          .reversed()
          .thenComparing(RankedPlace::titleKey)
          .thenComparing(RankedPlace::addressKey)
          .thenComparing(RankedPlace::idKey);

  private record RankedPlace(Place place, Set<String> placeCategories, double baseScore) {
    Set<String> matchedSelectedCategories(Set<String> selected) {
      Set<String> matched = new HashSet<>();
      if (selected == null || selected.isEmpty() || placeCategories == null || placeCategories.isEmpty()) {
        return matched;
      }
      for (String code : placeCategories) {
        if (selected.contains(code)) {
          matched.add(code);
        }
      }
      return matched;
    }

    String titleKey() {
      String value = place != null ? place.getTitle() : null;
      return value == null ? "" : value;
    }

    String addressKey() {
      String value = place != null ? place.getAddress() : null;
      return value == null ? "" : value;
    }

    String idKey() {
      String value = place != null ? place.getGooglePlaceId() : null;
      return value == null ? "" : value;
    }
  }
}
