package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.trip.EstimatedBudget;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BudgetService {

  private static final Map<String, Double> CATEGORY_COSTS = Map.ofEntries(
      Map.entry("restaurant", 30.0),
      Map.entry("cafe", 10.0),
      Map.entry("bakery", 8.0),
      Map.entry("museum", 15.0),
      Map.entry("art_gallery", 12.0),
      Map.entry("tourist_attraction", 10.0),
      Map.entry("historical_landmark", 10.0),
      Map.entry("park", 0.0),
      Map.entry("national_park", 5.0),
      Map.entry("beach", 0.0),
      Map.entry("hiking_area", 0.0),
      Map.entry("night_club", 40.0),
      Map.entry("bar", 25.0),
      Map.entry("casino", 50.0),
      Map.entry("amusement_park", 35.0),
      Map.entry("movie_theater", 15.0),
      Map.entry("bowling_alley", 20.0),
      Map.entry("stadium", 30.0),
      Map.entry("shopping_mall", 25.0),
      Map.entry("market", 15.0),
      Map.entry("gift_shop", 15.0),
      Map.entry("spa", 60.0),
      Map.entry("zoo", 20.0),
      Map.entry("aquarium", 20.0),
      Map.entry("winery", 25.0),
      Map.entry("brewery", 20.0),
      Map.entry("church", 0.0),
      Map.entry("mosque", 0.0),
      Map.entry("synagogue", 0.0),
      Map.entry("hindu_temple", 0.0),
      Map.entry("monument", 0.0)
  );

  private static final double DEFAULT_COST = 15.0;

  private static final Set<String> MEAL_CATEGORIES =
      Set.of("restaurant", "cafe", "bakery", "winery", "brewery");

  /**
   * Estimate cost for a single place based on its categories and Google price_level.
   * When priceLevel is available, adjust: baseCost * (1 + priceLevel * 0.3)
   */
  public double estimatePlaceCost(Place place) {
    double baseCost = DEFAULT_COST;
    if (place.getCategories() != null && !place.getCategories().isEmpty()) {
      baseCost = place.getCategories().stream()
          .map(cat -> CATEGORY_COSTS.getOrDefault(cat.getName(), DEFAULT_COST))
          .max(Double::compareTo)
          .orElse(DEFAULT_COST);
    }
    if (place.getPriceLevel() != null && place.getPriceLevel() > 0) {
      baseCost = baseCost * (1.0 + place.getPriceLevel() * 0.3);
    }
    return baseCost;
  }

  /**
   * Check if adding a place would keep the trip within budget.
   *
   * @param currentTotal running total of estimated costs so far
   * @param placeCost    estimated cost of the candidate place
   * @param budget       total trip budget
   * @return true if the place fits within budget
   */
  public boolean fitsWithinBudget(double currentTotal, double placeCost, int budget) {
    return (currentTotal + placeCost) <= budget;
  }

  /**
   * Build an EstimatedBudget breakdown from the list of planned activities.
   * Categories: activities (entry fees etc), meals (restaurants/cafes/bakeries),
   * transportation (estimated), accommodation (estimated per night), souvenirs.
   */
  public EstimatedBudget computeEstimatedBudget(List<Place> plannedPlaces, int tripDays) {
    double meals = 0.0;
    double activities = 0.0;

    for (Place place : plannedPlaces) {
      double cost = estimatePlaceCost(place);
      if (isMealPlace(place)) {
        meals += cost;
      } else {
        activities += cost;
      }
    }

    double transportation = tripDays * 15.0;
    double accommodation = tripDays * 80.0;
    double souvenirs = tripDays * 10.0;
    double total = meals + activities + transportation + accommodation + souvenirs;

    EstimatedBudget budget = new EstimatedBudget();
    budget.setMeals(meals);
    budget.setActivities(activities);
    budget.setTransportation(transportation);
    budget.setAccommodation(accommodation);
    budget.setSouvenirs(souvenirs);
    budget.setTotal(total);
    return budget;
  }

  private boolean isMealPlace(Place place) {
    if (place.getCategories() == null || place.getCategories().isEmpty()) {
      return false;
    }
    return place.getCategories().stream()
        .anyMatch(cat -> MEAL_CATEGORIES.contains(cat.getName().toLowerCase()));
  }
}
