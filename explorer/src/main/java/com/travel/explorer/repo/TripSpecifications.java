package com.travel.explorer.repo;

import com.travel.explorer.entities.City;
import com.travel.explorer.entities.Country;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.User;
import jakarta.persistence.criteria.Join;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TripSpecifications {

  private TripSpecifications() {}

  public static Specification<Trip> ownedByUser(Long userId) {
    return (root, query, cb) -> {
      Join<Trip, User> owner = root.join("owner");
      return cb.equal(owner.get("userId"), userId);
    };
  }

  public static Specification<Trip> isPublicTrip() {
    return (root, query, cb) -> cb.isTrue(root.get("isPublic"));
  }

  /** Trip has at least one of the given Google place category codes. */
  public static Specification<Trip> hasAnyCategory(List<String> codes) {
    return (root, query, cb) -> {
      query.distinct(true);
      return root.join("categories").in(codes);
    };
  }

  public static Specification<Trip> hasCountryId(Long countryId) {
    return (root, query, cb) -> {
      query.distinct(true);
      Join<Trip, City> cities = root.join("cities");
      Join<City, Country> country = cities.join("country");
      return cb.equal(country.get("id"), countryId);
    };
  }

  public static Specification<Trip> hasCountryNameIgnoreCase(String countryName) {
    return (root, query, cb) -> {
      query.distinct(true);
      Join<Trip, City> cities = root.join("cities");
      Join<City, Country> country = cities.join("country");
      return cb.equal(cb.lower(country.get("name")), countryName.toLowerCase());
    };
  }

  /**
   * Combines category and country filters. Returns {@code null} if nothing to filter.
   */
  public static Specification<Trip> fromFilters(
      List<String> categoryCodes, Long countryId, String countryName) {
    Specification<Trip> combined = null;

    if (categoryCodes != null && !categoryCodes.isEmpty()) {
      List<String> trimmed =
          categoryCodes.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
      if (!trimmed.isEmpty()) {
        combined = Specification.where(hasAnyCategory(trimmed));
      }
    }

    if (countryId != null) {
      Specification<Trip> countrySpec = hasCountryId(countryId);
      combined = combined == null ? countrySpec : combined.and(countrySpec);
    } else if (countryName != null && !countryName.isBlank()) {
      Specification<Trip> countrySpec = hasCountryNameIgnoreCase(countryName.trim());
      combined = combined == null ? countrySpec : combined.and(countrySpec);
    }

    return combined;
  }
}
