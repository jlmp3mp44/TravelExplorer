package com.travel.explorer.ui.trip;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.travel.explorer.google.GooglePlaceClient;
import com.travel.explorer.google.GooglePlacesExpectedData;
import com.travel.explorer.google.request.TextSearchRequest;
import com.travel.explorer.payload.place.GooglePlaceDto;
import java.net.http.HttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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
  private final GooglePlacesExpectedData placesExpectedData = new GooglePlacesExpectedData();

  @BeforeEach
  void setUpStubs() {
    GooglePlacesStubs.stubSuccessfulSearch();
    GooglePlacesStubs.stubSuccessfulGeocoding();
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
    loginSuccessfully();
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

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.google.GooglePlacesDataProvider#validSearchData")
  void shouldReturnPlaces(String testName, String query, String type, String language,
      WireMockRuntimeInfo wireMock) {
    // Avoid the JDK HTTP/2 cleartext upgrade when talking to the local mock server.
    HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    GooglePlaceClient client = new GooglePlaceClient("test-api-key", wireMock.getHttpBaseUrl(),
        new RestTemplateBuilder().requestFactory(() -> new JdkClientHttpRequestFactory(httpClient)));
    TextSearchRequest request = new TextSearchRequest(
        query, type, null, null, language, 20, null, true);

    var fullResponse = client.searchTextFull(request);
    assertNotNull(fullResponse);
    assertEquals(placesExpectedData.placeCount, fullResponse.getPlaces().size());
    assertPlace(fullResponse.getPlaces().get(0));

    var idsResponse = client.searchTextIds(request);
    assertNotNull(idsResponse);
    assertEquals(placesExpectedData.placeCount, idsResponse.getPlaces().size());
    assertEquals(placesExpectedData.placeId, idsResponse.getPlaces().get(0).getGooglePlaceId());
    assertNull(idsResponse.getNextPageToken());

    assertPlace(client.getPlaceDetails(idsResponse.getPlaces().get(0).getGooglePlaceId(), language));

    verify(2, postRequestedFor(urlPathEqualTo("/v1/places:searchText"))
        .withRequestBody(matchingJsonPath("$.textQuery", equalTo(query)))
        .withRequestBody(matchingJsonPath("$.includedType", equalTo(type)))
        .withHeader("X-Goog-Api-Key", equalTo("test-api-key")));
    verify(1, getRequestedFor(urlPathEqualTo("/v1/places/" + placesExpectedData.placeId))
        .withQueryParam("languageCode", equalTo(language)));
  }

  private void assertPlace(GooglePlaceDto place) {
    assertNotNull(place);
    assertEquals(placesExpectedData.placeId, place.getGooglePlaceId());
    assertEquals(placesExpectedData.name, place.getDisplayName().getText());
    assertEquals(placesExpectedData.primaryType, place.getPrimaryType());
    assertEquals(placesExpectedData.rating, place.getRating());
    assertEquals(placesExpectedData.latitude, place.getLocation().latitude());
    assertEquals(placesExpectedData.longitude, place.getLocation().longitude());
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.ui.trip.TripDataProvider#invalidDates")
  void shouldRejectInvalidDates(String testName, LocalDate start, LocalDate end,
      String expectedError) {
    openTripForm();
    if (start != null) {
      tripPage.setStartDate(start);
    }
    if (end != null) {
      tripPage.setEndDate(end);
    }
    tripPage.clickContinue();

    assertValidationPreventsProgress(expectedData.travelDatesTitle, expectedError);
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.ui.trip.TripDataProvider#missingRequiredTripData")
  void shouldRejectMissingRequiredTripData(String testName, LocalDate start, LocalDate end,
      String country, Integer budget, String currency, String expectedStep, String expectedError) {
    openTripForm();
    tripPage.setStartDate(start);
    tripPage.setEndDate(end);
    tripPage.clickContinue();
    assertThat(tripPage.getStepTitle()).hasText(expectedData.destinationTitle);

    if (country != null) {
      tripPage.selectCountry(country);
      tripPage.clickContinue();
      assertThat(tripPage.getStepTitle()).hasText(expectedData.budgetTitle);
      if (budget != null) {
        tripPage.setBudget(budget, currency);
        tripPage.clickContinue();
        assertThat(tripPage.getStepTitle()).hasText(expectedData.interestsTitle);
      }
    }
    tripPage.clickContinue();

    assertValidationPreventsProgress(expectedStep, expectedError);
  }

  private void openTripForm() {
    loginSuccessfully();
    tripPage = new TripPage(page);
    navigate(URL);
    tripPage.waitForPageTitle();
  }

  private void assertValidationPreventsProgress(String expectedStep, String expectedError) {
    assertThat(tripPage.getErrorMessage()).hasText(expectedError);
    assertThat(tripPage.getStepTitle()).hasText(expectedStep);
    assertThat(page).hasURL(URL);
    assertThat(tripPage.getCreateTripButton()).isHidden();
    verify(0, getRequestedFor(urlPathEqualTo("/maps/api/geocode/json")));
    verify(0, postRequestedFor(urlPathEqualTo("/v1/places:searchText")));
  }
}
