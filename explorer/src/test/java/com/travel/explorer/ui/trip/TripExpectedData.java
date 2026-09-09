package com.travel.explorer.ui.trip;

import java.util.regex.Pattern;

public class TripExpectedData {

  public final String travelDatesTitle = "Travel dates";
  public final String destinationTitle = "Destination";
  public final String budgetTitle = "Budget";
  public final String interestsTitle = "Interests";
  public final String paceTitle = "What pace do you prefer for your trip?";
  public final Pattern selectedInterestClass =
      Pattern.compile(".*\\binterest-chip--selected\\b.*");
  public final String selectedPaceAttribute = "true";
  public final Pattern createdTripTitle = Pattern.compile(".*\\S.*", Pattern.DOTALL);
}
