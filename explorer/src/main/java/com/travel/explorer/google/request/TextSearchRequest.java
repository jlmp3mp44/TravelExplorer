package com.travel.explorer.google.request;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for {@code POST https://places.googleapis.com/v1/places:searchText}.
 * Uses {@code includedType} (singular) — one type per request.
 * Uses {@code locationRestriction} (not bias) for strict radius limiting.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextSearchRequest(
    String textQuery,
    String includedType,
    LocationRestriction locationRestriction,
    String languageCode,
    int pageSize,
    String pageToken,
    Boolean strictTypeFiltering
) {}
