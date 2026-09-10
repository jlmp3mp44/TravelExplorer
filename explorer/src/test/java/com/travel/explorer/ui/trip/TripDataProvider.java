package com.travel.explorer.ui.trip;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class TripDataProvider {

  public static Stream<Arguments> invalidDates() {
    LocalDate today = LocalDate.now();
    return Stream.of(
        Arguments.of("Both dates missing", null, null, TripExpectedData.MISSING_DATES),
        Arguments.of("Start date missing", null, today.plusDays(3), TripExpectedData.MISSING_DATES),
        Arguments.of("End date missing", today, null, TripExpectedData.MISSING_DATES),
        Arguments.of("Start date in the past", today.minusDays(1), today.plusDays(3),
            TripExpectedData.PAST_DATES),
        Arguments.of("End date before start date", today.plusDays(3), today.plusDays(1),
            TripExpectedData.REVERSED_DATES)
    );
  }

  public static Stream<Arguments> missingRequiredTripData() {
    TripExpectedData expected = new TripExpectedData();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(3);
    return Stream.of(
        Arguments.of("Country missing", start, end, null, null, "EUR",
            expected.destinationTitle, TripExpectedData.MISSING_COUNTRY),
        Arguments.of("Budget missing", start, end, "Ukraine", null, "EUR",
            expected.budgetTitle, TripExpectedData.MISSING_BUDGET),
        Arguments.of("Interests missing", start, end, "Ukraine", 1000, "EUR",
            expected.interestsTitle, TripExpectedData.MISSING_INTERESTS)
    );
  }

  public static Stream<Arguments> validTripData() {

    LocalDate startDate = LocalDate.now();
    LocalDate endDate = startDate.plusDays(3);

    return Stream.of(
        Arguments.of(
            "Trip to Ukraine with museums and balanced pace",
            startDate,
            endDate,
            "Ukraine",
            1000,
            "EUR",
            "Culture & landmarks",
            "Museums",
            "Balanced"
        )
    );
  }
}
