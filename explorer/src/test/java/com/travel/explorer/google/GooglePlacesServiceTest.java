package com.travel.explorer.google;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.travel.explorer.google.request.TextSearchRequest;
import com.travel.explorer.payload.place.GooglePlaceDto;
import java.net.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@WireMockTest(httpPort = 8089)
class GooglePlacesServiceTest {

  private GooglePlaceClient client;
  private final GooglePlacesExpectedData expectedData = new GooglePlacesExpectedData();

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMock) {
    GooglePlacesStubs.stubSuccessfulSearch();
    // Avoid the JDK HTTP/2 cleartext upgrade when talking to the local mock server.
    HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    client = new GooglePlaceClient("test-api-key", wireMock.getHttpBaseUrl(),
        new RestTemplateBuilder().requestFactory(() -> new JdkClientHttpRequestFactory(httpClient)));
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.google.GooglePlacesDataProvider#validSearchData")
  void shouldReturnPlaces(String testName, String query, String type, String language) {
    TextSearchRequest request = new TextSearchRequest(
        query, type, null, null, language, 20, null, true);

    var fullResponse = client.searchTextFull(request);
    assertNotNull(fullResponse);
    assertEquals(expectedData.placeCount, fullResponse.getPlaces().size());
    assertPlace(fullResponse.getPlaces().get(0));

    var idsResponse = client.searchTextIds(request);
    assertNotNull(idsResponse);
    assertEquals(expectedData.placeCount, idsResponse.getPlaces().size());
    assertEquals(expectedData.placeId, idsResponse.getPlaces().get(0).getGooglePlaceId());
    assertNull(idsResponse.getNextPageToken());

    assertPlace(client.getPlaceDetails(idsResponse.getPlaces().get(0).getGooglePlaceId(), language));

    verify(2, postRequestedFor(urlPathEqualTo("/v1/places:searchText"))
        .withRequestBody(matchingJsonPath("$.textQuery", equalTo(query)))
        .withRequestBody(matchingJsonPath("$.includedType", equalTo(type)))
        .withHeader("X-Goog-Api-Key", equalTo("test-api-key")));
    verify(1, getRequestedFor(urlPathEqualTo("/v1/places/" + expectedData.placeId))
        .withQueryParam("languageCode", equalTo(language)));
  }

  private void assertPlace(GooglePlaceDto place) {
    assertNotNull(place);
    assertEquals(expectedData.placeId, place.getGooglePlaceId());
    assertEquals(expectedData.name, place.getDisplayName().getText());
    assertEquals(expectedData.primaryType, place.getPrimaryType());
    assertEquals(expectedData.rating, place.getRating());
    assertEquals(expectedData.latitude, place.getLocation().latitude());
    assertEquals(expectedData.longitude, place.getLocation().longitude());
  }
}
