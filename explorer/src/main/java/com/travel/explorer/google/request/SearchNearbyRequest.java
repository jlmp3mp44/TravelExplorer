package com.travel.explorer.google.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Request body for {@code POST https://places.googleapis.com/v1/places:searchNearby}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchNearbyRequest(
    LocationRestriction locationRestriction,
    int maxResultCount,
    /** Table A place types; OR semantics — at least one match. Up to 50 per request. */
    List<String> includedTypes,
    String languageCode
) {}

