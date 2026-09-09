package com.travel.explorer.google;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class GooglePlacesDataProvider {

  public static Stream<Arguments> validSearchData() {
    return Stream.of(Arguments.of("Museums in Kyiv", "museums in Kyiv, Ukraine", "museum", "en"));
  }
}
