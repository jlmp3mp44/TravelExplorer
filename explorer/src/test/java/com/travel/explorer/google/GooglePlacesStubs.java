package com.travel.explorer.google;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class GooglePlacesStubs {

  public static void stubSuccessfulGeocoding() {
    stubFor(get(urlPathEqualTo("/maps/api/geocode/json"))
        .withQueryParam("address", matching(".+"))
        .withQueryParam("key", equalTo("test-api-key"))
        .willReturn(okJsonFromFile("google_geocoding_success.json")));
  }

  public static void stubSuccessfulSearch() {
    stubFor(post(urlPathEqualTo("/v1/places:searchText"))
        .withHeader("X-Goog-Api-Key", matching(".+"))
        .withHeader("X-Goog-FieldMask", containing("places.displayName"))
        .withRequestBody(matchingJsonPath("$.textQuery"))
        .willReturn(okJsonFromFile("google_places_success.json")));

    stubFor(post(urlPathEqualTo("/v1/places:searchText"))
        .withHeader("X-Goog-Api-Key", matching(".+"))
        .withHeader("X-Goog-FieldMask", equalTo("places.id,nextPageToken"))
        .withRequestBody(matchingJsonPath("$.textQuery"))
        .willReturn(okJsonFromFile("google_places_ids.json")));

    stubFor(get(urlPathEqualTo("/v1/places/test-museum-1"))
        .withHeader("X-Goog-Api-Key", matching(".+"))
        .withQueryParam("languageCode", matching(".+"))
        .willReturn(okJsonFromFile("google_place_details.json")));
  }

  private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder okJsonFromFile(
      String filename) {
    return aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBodyFile(filename);
  }
}
