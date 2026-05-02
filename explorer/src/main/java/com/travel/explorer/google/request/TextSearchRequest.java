package com.travel.explorer.google.request;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for {@code POST https://places.googleapis.com/v1/places:searchText}.
 * Uses {@code includedType} (singular) — one type per request.
 *
 * <p>Per Google: {@code locationRestriction} accepts only a <em>rectangle</em>; a {@code circle}
 * must be sent as {@code locationBias}, not {@code locationRestriction}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextSearchRequest(
    String textQuery,
    String includedType,
    LocationBias locationBias,
    String languageCode,
    int pageSize,
    String pageToken,
    Boolean strictTypeFiltering
) {}
