package com.travel.explorer.ui.trip;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class TripDataProvider {

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
