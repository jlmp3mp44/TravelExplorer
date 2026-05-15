package com.travel.explorer.entities;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * User-selected pace / density for the trip (wizard last step).
 *
 * <p>{@link #LOW} is the “relaxed” preset: fewer stops per day and extra buffer between them.
 */
public enum TripIntensity {
  @JsonAlias({"RELAXED", "relaxed", "Relaxed"})
  LOW,
  MEDIUM,
  HIGH
}
