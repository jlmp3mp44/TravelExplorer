package com.travel.explorer.ui.trip;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.travel.explorer.BasePage;
import com.travel.explorer.config.TestConfig;
import com.travel.explorer.ui.BaseTest;
import com.travel.explorer.ui.login.LoginTest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TripTest extends BaseTest{

  private String URL = TestConfig.getBaseUrl() + "/trip";
  TripPage tripPage;

  @BeforeEach
  void login() {
    loginSuccessfully();
  }


  @ParameterizedTest()
  @MethodSource("com.travel.explorer.ui.trip.TripDataProvider#validTravelDates"
  )
  void shouldCreateTripSuccessfully(LocalDate startDate, LocalDate endDate) {
    tripPage = new TripPage(page);
    navigate(URL);
    tripPage.waitForPageTitle();
    assertTrue(tripPage.isPageTitleVisible());
    assertTrue(tripPage.isTravelDatesTitleVisible());

    tripPage.setStartDate(startDate);
    tripPage.setEndDate(endDate);
    tripPage.clickContinue();

    page.pause();
  }

}
