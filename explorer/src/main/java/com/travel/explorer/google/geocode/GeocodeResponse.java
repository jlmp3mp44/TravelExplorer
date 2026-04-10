package com.travel.explorer.google.geocode;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GeocodeResponse(
    String status,
    @JsonProperty("error_message") String errorMessage,
    List<GeocodeResult> results
) {}
