package com.travel.explorer.service.scheduling;

import com.travel.explorer.entities.Category;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Google Places category types to estimated visit durations.
 */
public final class CategoryDuration {
    private CategoryDuration() {}

    private static final Map<String, Double> DURATIONS_HOURS = Map.ofEntries(
        Map.entry("cafe", 0.5),
        Map.entry("bakery", 0.5),
        Map.entry("gift_shop", 0.5),
        Map.entry("market", 0.5),
        Map.entry("museum", 1.0),
        Map.entry("art_gallery", 1.0),
        Map.entry("restaurant", 1.0),
        Map.entry("bowling_alley", 1.0),
        Map.entry("movie_theater", 1.0),
        Map.entry("tourist_attraction", 1.0),
        Map.entry("historical_landmark", 1.0),
        Map.entry("monument", 1.0),
        Map.entry("church", 1.0),
        Map.entry("mosque", 1.0),
        Map.entry("synagogue", 1.0),
        Map.entry("hindu_temple", 1.0),
        Map.entry("shopping_mall", 1.5),
        Map.entry("zoo", 1.5),
        Map.entry("aquarium", 1.5),
        Map.entry("winery", 1.5),
        Map.entry("brewery", 1.5),
        Map.entry("stadium", 1.5),
        Map.entry("park", 2.0),
        Map.entry("hiking_area", 2.0),
        Map.entry("national_park", 2.0),
        Map.entry("beach", 2.0),
        Map.entry("amusement_park", 2.0),
        Map.entry("night_club", 2.0),
        Map.entry("bar", 2.0),
        Map.entry("casino", 2.0),
        Map.entry("spa", 2.0)
    );
    private static final double DEFAULT_HOURS = 1.0;

    /** Get estimated visit duration in hours for a place, based on its primary type or categories. */
    public static double durationHours(String primaryType, List<Category> categories) {
        if (primaryType != null) {
            String key = primaryType.trim().toLowerCase(Locale.ROOT);
            Double d = DURATIONS_HOURS.get(key);
            if (d != null) return d;
        }
        if (categories != null) {
            double max = -1;
            for (Category cat : categories) {
                if (cat.getName() != null) {
                    Double d = DURATIONS_HOURS.get(cat.getName().trim().toLowerCase(Locale.ROOT));
                    if (d != null && d > max) max = d;
                }
            }
            if (max > 0) return max;
        }
        return DEFAULT_HOURS;
    }
}
