package com.travel.explorer.google.request;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Circle (searchNearby) or rectangle (searchText strict area). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationRestriction(Circle circle, Rectangle rectangle) {

  public LocationRestriction(Circle circle) {
    this(circle, null);
  }

  public static LocationRestriction rectangle(Rectangle rectangle) {
    return new LocationRestriction(null, rectangle);
  }
}
