package com.travel.explorer.ui.trip;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.travel.explorer.config.TestConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.travel.explorer.google.GooglePlacesStubs;
import com.travel.explorer.ui.BaseTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@WireMockTest(httpPort = 8089)
public class TripTest extends BaseTest{

  private String URL = TestConfig.getBaseUrl() + "/trip";
  TripPage tripPage;
  TripExpectedData expectedData = new TripExpectedData();

  @BeforeEach
  void login() {
    GooglePlacesStubs.stubSuccessfulSearch();
    GooglePlacesStubs.stubSuccessfulGeocoding();
    loginSuccessfully();
  }


  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.ui.trip.TripDataProvider#validTripData"
  )
  void shouldCreateTripSuccessfully(
      String testName,
      LocalDate startDate,
      LocalDate endDate,
      String country,
      int budget,
      String currency,
      String category,
      String interest,
      String pace
  ) {
    tripPage = new TripPage(page);
    navigate(URL);
    tripPage.waitForPageTitle();
    assertThat(tripPage.getStepTitle()).hasText(expectedData.travelDatesTitle);

    tripPage.setStartDate(startDate);
    tripPage.setEndDate(endDate);
    tripPage.clickContinue();

    assertThat(tripPage.getStepTitle()).hasText(expectedData.destinationTitle);
    tripPage.selectCountry(country);
    tripPage.clickContinue();

    assertThat(tripPage.getStepTitle()).hasText(expectedData.budgetTitle);
    tripPage.setBudget(budget, currency);
    tripPage.clickContinue();

    assertThat(tripPage.getStepTitle()).hasText(expectedData.interestsTitle);
    tripPage.selectInterest(category, interest);
    assertThat(tripPage.getInterest(interest)).hasClass(expectedData.selectedInterestClass);
    tripPage.clickContinue();

    assertThat(tripPage.getStepTitle()).hasText(expectedData.paceTitle);
    tripPage.selectPace(pace);
    assertThat(tripPage.getPace(pace)).hasAttribute("aria-pressed", expectedData.selectedPaceAttribute);
    assertThat(tripPage.getCreateTripButton()).isVisible();
    assertThat(tripPage.getCreateTripButton()).isEnabled();

    tripPage.clickCreateTrip();
    assertThat(tripPage.getCreatedTripTitle()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(60000));
    assertThat(tripPage.getCreatedTripTitle()).hasText(expectedData.createdTripTitle);
    verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo("/maps/api/geocode/json")));
    verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo("/v1/places:searchText")));
  }

}
