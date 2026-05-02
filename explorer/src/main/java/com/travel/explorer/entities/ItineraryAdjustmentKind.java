package com.travel.explorer.entities;

public enum ItineraryAdjustmentKind {
  REMOVE,
  ADD,
  /** Activity kept; primary place swapped (manual replace / smart replace). */
  REPLACE
}
