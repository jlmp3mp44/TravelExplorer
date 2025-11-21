package com.travel.explorer.payload.trip;

import lombok.Data;
import java.util.List;

@Data
public class AITripResponce {
  private String tripTitle;
  private List<TripDay> days;
  private EstimatedBudget estimatedBudget;
}
