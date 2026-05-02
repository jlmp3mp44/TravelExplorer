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
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlaceRecommendationService {

  private static final double DIVERSITY_PENALTY = 0.3;

  private final ContentBasedScorer contentBasedScorer;
  private final CollaborativeFilteringClient collaborativeFilteringClient;

  @Value("${recommendation.content.weight:0.6}")
  private double contentWeight;

  @Value("${recommendation.svd.weight:0.4}")
  private double svdWeight;

  public PlaceRecommendationService(
      ContentBasedScorer contentBasedScorer,
      CollaborativeFilteringClient collaborativeFilteringClient) {
    this.contentBasedScorer = contentBasedScorer;
    this.collaborativeFilteringClient = collaborativeFilteringClient;
  }

  public List<Place> rankPlaces(
      List<Place> places, List<String> selectedCategoryCodes, Long userId) {
    return rankPlaces(places, selectedCategoryCodes, userId, null);
  }

  /**
   * @param replacementCategoryFocus optional normalized category / type names to favor when
   *     replacing an activity (same-category swaps).
   */
  public List<Place> rankPlaces(
      List<Place> places,
      List<String> selectedCategoryCodes,
      Long userId,
      Set<String> replacementCategoryFocus) {
    if (places == null || places.isEmpty()) {
      return List.of();
    }

    // Filter out permanently/temporarily closed places
    List<Place> openPlaces = new ArrayList<>(places.size());
    for (Place place : places) {
      if (Boolean.TRUE.equals(place.getPermanentlyClosed())) continue;
      if (Boolean.TRUE.equals(place.getTemporarilyClosed())) continue;
      String status = place.getBusinessStatus();
      if (status != null) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("CLOSED_PERMANENTLY".equals(normalized) || "CLOSED_TEMPORARILY".equals(normalized)) {
          continue;
        }
      }
      openPlaces.add(place);
    }
    if (openPlaces.isEmpty()) {
      return List.of();
    }

    Set<String> selected = normalizeToSet(selectedCategoryCodes);

    // Compute content-based scores
    Map<Place, Double> contentScores = new HashMap<>(openPlaces.size());
    List<Long> placeIdsForSvd = new ArrayList<>();
    for (Place place : openPlaces) {
      contentScores.put(
          place,
          contentBasedScorer.scoreWithReplacementFocus(place, selected, replacementCategoryFocus));
      if (place.getId() != null) {
        placeIdsForSvd.add(place.getId());
      }
    }

    // Fetch SVD predictions if userId is provided
    Map<Long, Double> svdPredictions =
        (userId != null && !placeIdsForSvd.isEmpty())
            ? collaborativeFilteringClient.predictRatings(userId, placeIdsForSvd)
            : Map.of();

    // Build ranked entries with blended scores
    List<RankedPlace> remaining = new ArrayList<>(openPlaces.size());
    for (Place place : openPlaces) {
      double contentScore = contentScores.get(place);
      double normalizedContent = Math.min(5.0, Math.max(0.0, contentScore / 100.0));

      double finalScore;
      Double svdScore =
          (place.getId() != null) ? svdPredictions.get(place.getId()) : null;
      if (svdScore != null) {
        finalScore = contentWeight * normalizedContent + svdWeight * svdScore;
      } else {
        // Cold-start or no ID: use content-based only
        finalScore = normalizedContent;
      }

      Set<String> placeCategories = extractCategoryCodes(place);
      remaining.add(new RankedPlace(place, placeCategories, finalScore));
    }
    remaining.sort(BASE_COMPARATOR);

    // Greedy diversity-aware selection
    Map<String, Integer> categoryUsage = new HashMap<>();
    List<Place> ranked = new ArrayList<>(remaining.size());
    while (!remaining.isEmpty()) {
      RankedPlace next = chooseNext(remaining, categoryUsage);
      remaining.remove(next);
      double adjusted = adjustedScore(next, categoryUsage);
      next.place().setTotalScore((int) Math.round(adjusted * 100));
      ranked.add(next.place());
      for (String code : next.matchedSelectedCategories(selected)) {
        categoryUsage.merge(code, 1, Integer::sum);
      }
    }
    return ranked;
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
      } else if (adjusted == bestScore
          && best != null
          && BASE_COMPARATOR.compare(candidate, best) < 0) {
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

  private static final Comparator<RankedPlace> BASE_COMPARATOR =
      Comparator.comparingDouble(RankedPlace::baseScore)
          .reversed()
          .thenComparing(RankedPlace::titleKey)
          .thenComparing(RankedPlace::addressKey)
          .thenComparing(RankedPlace::idKey);

  private record RankedPlace(Place place, Set<String> placeCategories, double baseScore) {
    Set<String> matchedSelectedCategories(Set<String> selected) {
      Set<String> matched = new HashSet<>();
      if (selected == null
          || selected.isEmpty()
          || placeCategories == null
          || placeCategories.isEmpty()) {
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
