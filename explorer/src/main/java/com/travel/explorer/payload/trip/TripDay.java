package com.travel.explorer.payload.trip;

import lombok.Data;
import java.util.List;

@Data
public class TripDay {
  private int day;
  private String location;
  private List<String> activities;
}
