package com.travel.explorer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.travel.explorer.entities.Category;
import com.travel.explorer.entities.Place;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceRecommendationServiceTest {

  private PlaceRecommendationService service;

  @BeforeEach
  void setUp() {
    service =
        new PlaceRecommendationService(
            new ContentBasedScorer(), mock(CollaborativeFilteringClient.class));
    ReflectionTestUtils.setField(service, "contentWeight", 0.6);
    ReflectionTestUtils.setField(service, "svdWeight", 0.4);
  }

  @Test
  void relevantPlaceOutranksIrrelevantEvenWithHigherRating() {
    Place relevantMuseum = place("Museum Place", "museum", 3.4, 20, "OPERATIONAL");
    Place irrelevantCafe = place("Top Cafe", "cafe", 5.0, 5000, "OPERATIONAL");

    List<Place> ranked =
        service.rankPlaces(List.of(relevantMuseum, irrelevantCafe), List.of("museum"), null);

    assertEquals("Museum Place", ranked.get(0).getTitle());
    assertTrue(ranked.get(0).getTotalScore() > ranked.get(1).getTotalScore());
  }

  @Test
  void permanentlyClosedPlacesAreExcludedFromRanking() {
    Place operationalMuseum = place("Open Museum", "museum", 4.0, 100, "OPERATIONAL");
    Place closedMuseum = place("Closed Museum", "museum", 4.0, 100, "CLOSED_PERMANENTLY");

    List<Place> ranked =
        service.rankPlaces(List.of(closedMuseum, operationalMuseum), List.of("museum"), null);

    assertEquals(1, ranked.size());
    assertEquals("Open Museum", ranked.get(0).getTitle());
  }

  @Test
  void diversityPenaltyPromotesDifferentCategoryWhenScoresAreClose() {
    Place museumOne = place("Museum A", "museum", 4.7, 250, "OPERATIONAL");
    Place museumTwo = place("Museum B", "museum", 4.6, 220, "OPERATIONAL");
    Place cityPark = place("City Park", "park", 4.1, 80, "OPERATIONAL");

    List<Place> ranked =
        service.rankPlaces(
            List.of(museumOne, museumTwo, cityPark), List.of("museum", "park"), null);

    assertEquals("Museum A", ranked.get(0).getTitle());
    assertEquals("City Park", ranked.get(1).getTitle());
  }

  private Place place(
      String title, String categoryCode, double rating, int userRatingCount, String businessStatus) {
    Place place = new Place();
    place.setTitle(title);
    place.setAddress("Address " + title);
    place.setPrimaryType(categoryCode);
    place.setRating(rating);
    place.setUserRatingCount(userRatingCount);
    place.setBusinessStatus(businessStatus);
    place.setCategories(new ArrayList<>(List.of(new Category(null, categoryCode))));
    return place;
  }
}
