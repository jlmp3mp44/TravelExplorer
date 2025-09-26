package com.travel.explorer.payload;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripListResponce {
  private List<TripResponce> content;
  private Integer pageNumber;
  private Integer pageSize;
  private Long totalElements;
  private Integer totalPages;
  private boolean lastPage;
}
