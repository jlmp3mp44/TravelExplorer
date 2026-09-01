package com.travel.explorer.ui.trip;

import com.travel.explorer.ui.ExistingUser;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class TripDataProvider {

  public static Stream<Arguments> validTravelDates() {

    LocalDate startDate = LocalDate.now();
    LocalDate endDate = startDate.plusDays(3);

    return Stream.of(
        Arguments.of(startDate, endDate)
    );
  }
}
