package com.travel.explorer.payload.trip;

import lombok.Data;

@Data
public class EstimatedBudget {
  private double accommodation;
  private double meals;
  private double transportation;
  private double activities;
  private double souvenirs;
  private double total;
}
