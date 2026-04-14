package com.travel.explorer.place;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Allowed Google Places <a href="https://developers.google.com/maps/documentation/places/web-service/place-types">Table A</a>
 * types for trip interests. Only these values are accepted from clients and used with the Places API.
 */
public enum PlaceInterestType {
  TOURIST_ATTRACTION(
      "tourist_attraction", "Main tourist attractions", PlaceInterestGroup.CULTURE),
  MUSEUM("museum", "Museums", PlaceInterestGroup.CULTURE),
  ART_GALLERY("art_gallery", "Art galleries", PlaceInterestGroup.CULTURE),
  HISTORICAL_LANDMARK(
      "historical_landmark", "Historical landmarks", PlaceInterestGroup.CULTURE),
  CHURCH("church", "Churches", PlaceInterestGroup.CULTURE),
  HINDU_TEMPLE("hindu_temple", "Hindu temples", PlaceInterestGroup.CULTURE),
  MOSQUE("mosque", "Mosques", PlaceInterestGroup.CULTURE),
  SYNAGOGUE("synagogue", "Synagogues", PlaceInterestGroup.CULTURE),
  MONUMENT("monument", "Monuments", PlaceInterestGroup.CULTURE),

  PARK("park", "Parks", PlaceInterestGroup.NATURE),
  NATIONAL_PARK("national_park", "National parks", PlaceInterestGroup.NATURE),
  AQUARIUM("aquarium", "Aquariums", PlaceInterestGroup.NATURE),
  ZOO("zoo", "Zoos", PlaceInterestGroup.NATURE),
  BEACH("beach", "Beaches", PlaceInterestGroup.NATURE),
  HIKING_AREA("hiking_area", "Hiking areas", PlaceInterestGroup.NATURE),

  AMUSEMENT_PARK("amusement_park", "Amusement parks", PlaceInterestGroup.ENTERTAINMENT),
  MOVIE_THEATER("movie_theater", "Movie theaters", PlaceInterestGroup.ENTERTAINMENT),
  NIGHT_CLUB("night_club", "Night clubs", PlaceInterestGroup.ENTERTAINMENT),
  BAR("bar", "Bars", PlaceInterestGroup.ENTERTAINMENT),
  CASINO("casino", "Casinos", PlaceInterestGroup.ENTERTAINMENT),
  BOWLING_ALLEY("bowling_alley", "Bowling", PlaceInterestGroup.ENTERTAINMENT),
  STADIUM("stadium", "Stadiums & sports arenas", PlaceInterestGroup.ENTERTAINMENT),

  RESTAURANT("restaurant", "Restaurants", PlaceInterestGroup.GASTRONOMY),
  CAFE("cafe", "Cafes", PlaceInterestGroup.GASTRONOMY),
  BAKERY("bakery", "Bakeries", PlaceInterestGroup.GASTRONOMY),
  WINERY("winery", "Wineries", PlaceInterestGroup.GASTRONOMY),
  BREWERY("brewery", "Breweries", PlaceInterestGroup.GASTRONOMY),

  SHOPPING_MALL("shopping_mall", "Shopping malls", PlaceInterestGroup.SHOPPING),
  MARKET("market", "Markets", PlaceInterestGroup.SHOPPING),
  GIFT_SHOP("gift_shop", "Gift shops", PlaceInterestGroup.SHOPPING),
  SPA("spa", "Spas & wellness", PlaceInterestGroup.SHOPPING);

  private static final Map<String, PlaceInterestType> BY_CODE;

  static {
    Map<String, PlaceInterestType> m = new LinkedHashMap<>();
    for (PlaceInterestType t : values()) {
      m.put(t.code, t);
    }
    BY_CODE = Collections.unmodifiableMap(m);
  }

  private final String code;
  private final String label;
  private final PlaceInterestGroup group;

  PlaceInterestType(String code, String label, PlaceInterestGroup group) {
    this.code = code;
    this.label = label;
    this.group = group;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public PlaceInterestGroup getGroup() {
    return group;
  }

  public static PlaceInterestType fromCode(String code) {
    if (code == null) {
      return null;
    }
    return BY_CODE.get(code.trim());
  }

  public static boolean isAllowedCode(String code) {
    return fromCode(code) != null;
  }

  public static List<PlaceInterestGroup> groupsInOrder() {
    return List.of(PlaceInterestGroup.values());
  }

  public static Map<PlaceInterestGroup, List<PlaceInterestType>> groupedByGroup() {
    return Arrays.stream(values())
        .collect(
            Collectors.groupingBy(
                PlaceInterestType::getGroup, LinkedHashMap::new, Collectors.toList()));
  }

  public static List<PlaceInterestType> interestsInGroup(PlaceInterestGroup group) {
    if (group == null) {
      return List.of();
    }
    return Arrays.stream(values())
        .filter(t -> t.group == group)
        .collect(Collectors.toList());
  }
}
