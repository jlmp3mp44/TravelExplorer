package com.travel.explorer.google.request;

/** Axis-aligned bounds for {@code locationRestriction.rectangle}. */
public record Rectangle(LatLngPoint low, LatLngPoint high) {}
