package com.travel.explorer.google.request;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Soft geographic bias for Text Search (API supports circle or rectangle here).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationBias(Circle circle) {}
