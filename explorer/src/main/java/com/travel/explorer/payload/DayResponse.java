package com.travel.explorer.payload;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayResponse {

  private Integer id;

  private LocalDate date;
  private List<ActivityResponse> activities = new ArrayList<>();
}
