package com.travel.explorer.service.scheduling;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Determines the active time window for a day based on trip categories.
 */
public final class DayTimeWindow {
    private DayTimeWindow() {}

    private static final Set<String> NIGHTLIFE = Set.of("night_club", "bar", "casino");
    private static final Set<String> EARLY_MORNING = Set.of("hiking_area", "national_park", "beach");

    private static final int DEFAULT_START_MIN = 9 * 60;   // 09:00
    private static final int DEFAULT_END_MIN = 21 * 60;    // 21:00
    private static final int EARLY_START_MIN = 7 * 60;     // 07:00
    private static final int LATE_END_MIN = 26 * 60;       // 02:00 next day

    /**
     * Compute start/end times in minutes-from-midnight for a given day.
     * End can exceed 24*60 (e.g. 26*60 = 2 AM next day) for nightlife trips.
     *
     * @return int[]{startMinutes, endMinutes}
     */
    public static int[] windowMinutes(List<String> tripCategories) {
        boolean hasNightlife = false;
        boolean hasEarlyOutdoor = false;
        if (tripCategories != null) {
            for (String cat : tripCategories) {
                String lc = cat.trim().toLowerCase(Locale.ROOT);
                if (NIGHTLIFE.contains(lc)) hasNightlife = true;
                if (EARLY_MORNING.contains(lc)) hasEarlyOutdoor = true;
            }
        }

        int startMin = DEFAULT_START_MIN;
        int endMin = DEFAULT_END_MIN;

        if (hasEarlyOutdoor) {
            startMin = EARLY_START_MIN;
        }
        if (hasNightlife) {
            endMin = LATE_END_MIN;
        }

        return new int[]{startMin, endMin};
    }
}
