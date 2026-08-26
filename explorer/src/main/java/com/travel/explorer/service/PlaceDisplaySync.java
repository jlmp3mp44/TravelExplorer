package com.travel.explorer.service;

import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.GooglePlaceDto;

/** Copies human-readable fields from a fresh Google result onto a persisted {@link Place}. */
public final class PlaceDisplaySync {

  private PlaceDisplaySync() {}

  public static boolean titleNeedsEnglishRefresh(String title) {
    if (title == null || title.isBlank()) {
      return true;
    }
    return title.codePoints().anyMatch(PlaceDisplaySync::isCyrillic);
  }

  public static void applyFreshDisplay(Place target, GooglePlaceDto dto) {
    if (target == null || dto == null) {
      return;
    }
    if (dto.getDisplayName() != null) {
      String text = dto.getDisplayName().getText();
      if (text != null && !text.isBlank()) {
        target.setTitle(text.trim());
      }
    }
    if (dto.getFormattedAddress() != null && !dto.getFormattedAddress().isBlank()) {
      target.setAddress(dto.getFormattedAddress().trim());
    }
  }

  public static void applyFreshDisplay(Place target, Place source) {
    if (target == null || source == null) {
      return;
    }
    if (source.getTitle() != null && !source.getTitle().isBlank()) {
      target.setTitle(source.getTitle().trim());
    }
    if (source.getAddress() != null && !source.getAddress().isBlank()) {
      target.setAddress(source.getAddress().trim());
    }
  }

  private static boolean isCyrillic(int codePoint) {
    return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.CYRILLIC;
  }
}
