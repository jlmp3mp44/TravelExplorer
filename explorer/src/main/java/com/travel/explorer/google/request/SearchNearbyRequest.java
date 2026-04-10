package com.travel.explorer.google.request;


public record SearchNearbyRequest(
    LocationRestriction locationRestriction,
    int maxResultCount
) {}

