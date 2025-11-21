package com.travel.explorer.payload.trip;

import java.util.List;
import lombok.Data;

@Data
public class TripResponce {
  private String title;
  private String desc;
  private String startDate;
  private String endDate;
  private List<String> placeTitles;
}
