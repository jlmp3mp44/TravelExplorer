package com.travel.explorer.payload;

import java.util.List;
import lombok.Data;

@Data
public class TripResponce {
  private String title;
  private String desc;
  private String startDate;
  private String endDate;
  private List<String> placeTitles; // тут тільки id існуючих places
}
