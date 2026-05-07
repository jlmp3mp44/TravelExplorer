package com.travel.explorer.entities;

/** Why the user is swapping this activity for their own itinerary view. */
public enum ActivityChangeReason {
  /** User has already been to the suggested place. */
  WAS_HERE,
  /** User does not want to visit the suggested place. */
  DONT_WANT_TO_GO,
  /** New activity appended (audit for ADD kind; not a user-facing swap reason). */
  ADDED
}
