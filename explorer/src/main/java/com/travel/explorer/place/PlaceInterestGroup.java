package com.travel.explorer.place;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * High-level category groups for UI (pick a group, then pick Google place type interests).
 */
public enum PlaceInterestGroup {
  CULTURE("culture", "Culture & landmarks"),
  NATURE("nature", "Nature & leisure"),
  ENTERTAINMENT("entertainment", "Entertainment & nightlife"),
  GASTRONOMY("gastronomy", "Food & drink"),
  SHOPPING("shopping", "Shopping & services");

  private static final Map<String, PlaceInterestGroup> BY_ID =
      Arrays.stream(values())
          .collect(Collectors.toMap(PlaceInterestGroup::getId, Function.identity()));

  private final String id;
  private final String title;

  PlaceInterestGroup(String id, String title) {
    this.id = id;
    this.title = title;
  }

  /** Stable id for URLs and JSON, e.g. {@code culture}. */
  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public static PlaceInterestGroup fromId(String id) {
    if (id == null) {
      return null;
    }
    return BY_ID.get(id.trim().toLowerCase());
  }
}
