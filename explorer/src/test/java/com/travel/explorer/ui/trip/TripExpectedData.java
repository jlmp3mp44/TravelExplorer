package com.travel.explorer.ui.trip;

import java.util.regex.Pattern;

public class TripExpectedData {

  public static final String MISSING_DATES = "Please choose both a start date and an end date.";
  public static final String PAST_DATES = "Pick dates from today onward.";
  public static final String REVERSED_DATES = "The end date can\u2019t be before the start date.";
  public static final String MISSING_COUNTRY = "Please choose a country to continue.";
  public static final String MISSING_BUDGET = "Please enter your budget.";
  public static final String MISSING_INTERESTS = "Choose at least one interest from any group.";

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
