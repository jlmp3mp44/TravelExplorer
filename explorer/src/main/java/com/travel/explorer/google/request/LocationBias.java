package com.travel.explorer.google.request;

/**
 * Location bias for Text Search API. Same circle structure as LocationRestriction.
 */
public record LocationBias(Circle circle) {}
