package com.travel.explorer.google.response;

import com.travel.explorer.payload.place.GooglePlaceDto;
import java.util.List;
import lombok.Data;

@Data
public class GooglePlacesResponse {

  private List<GooglePlaceDto> places;

  /** Pagination token for the next page. Null when no more results. */
  private String nextPageToken;
}
