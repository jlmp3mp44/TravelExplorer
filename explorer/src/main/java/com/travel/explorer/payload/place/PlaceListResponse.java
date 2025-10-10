package com.travel.explorer.payload.place;

import com.travel.explorer.payload.TripResponce;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceListResponse {
  private List<PlaceResponse> content;
  private Integer pageNumber;
  private Integer pageSize;
  private Long totalElements;
  private Integer totalPages;
  private boolean lastPage;
}
