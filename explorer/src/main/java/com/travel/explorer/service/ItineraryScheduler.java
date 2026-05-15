package com.travel.explorer.service;

import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.Place;
import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.TripIntensity;
import com.travel.explorer.entities.embeddable.OpenHours;
import com.travel.explorer.service.scheduling.CategoryDuration;
import com.travel.explorer.service.scheduling.DayTimeWindow;
import com.travel.explorer.service.scheduling.HaversineUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class ItineraryScheduler {

    /**
     * Build the trip's day/activity structure from a ranked list of candidate places,
     * respecting open hours, travel time, and budget constraints.
     *
     * @param trip the trip being built (start/endDate, categories must be set)
     * @param rankedPlaces ordered list of recommended places (best first)
     * @param budgetService for cost checking
     * @param totalBudget the user's budget
     * @return list of Days with Activities populated, and total estimated cost
     */
    public ScheduleResult schedule(Trip trip, List<Place> rankedPlaces,
                                    BudgetService budgetService, int totalBudget) {
        TripIntensity pace = trip.getIntensity() != null ? trip.getIntensity() : TripIntensity.MEDIUM;
        int maxActivitiesThisDay = maxActivitiesPerDay(pace);
        int restAfterActivityMin = restMinutesBetweenStops(pace);

        List<String> categories = trip.getCategories();
        int[] window = DayTimeWindow.windowMinutes(categories);
        int dayStartMin = window[0];
        int dayEndMin = window[1];

        List<Day> days = new ArrayList<>();
        double runningCost = 0.0;
        Set<Long> usedPlaceIds = new LinkedHashSet<>();
        Set<String> usedPlaceKeys = new LinkedHashSet<>();

        for (LocalDate date = trip.getStartDate(); !date.isAfter(trip.getEndDate()); date = date.plusDays(1)) {
            Day day = new Day();
            day.setDate(date);
            day.setTrip(trip);

            int currentTimeMin = dayStartMin;
            Place lastPlace = null;
            int sortOrder = 0;
            int activitiesToday = 0;

            for (int i = 0;
                 i < rankedPlaces.size()
                     && currentTimeMin < dayEndMin
                     && activitiesToday < maxActivitiesThisDay;
                 i++) {
                Place candidate = rankedPlaces.get(i);

                // Skip if already used (by persisted ID or dedup key for transient places)
                if (candidate.getId() != null && usedPlaceIds.contains(candidate.getId())) {
                    continue;
                }
                String dedupKey = buildDedupKey(candidate);
                if (dedupKey != null && usedPlaceKeys.contains(dedupKey)) {
                    continue;
                }

                // Estimate cost
                double placeCost = budgetService.estimatePlaceCost(candidate);
                if (!budgetService.fitsWithinBudget(runningCost, placeCost, totalBudget)) {
                    continue; // skip expensive place, try next
                }

                // Travel time from last place
                double travelMin = 0;
                if (lastPlace != null && lastPlace.getLocation() != null && candidate.getLocation() != null) {
                    travelMin = HaversineUtil.travelTimeMinutes(
                        lastPlace.getLocation().getLat(), lastPlace.getLocation().getLng(),
                        candidate.getLocation().getLat(), candidate.getLocation().getLng());
                    travelMin = Math.max(travelMin, 10); // minimum 10 min buffer
                }

                int arrivalMin = currentTimeMin + (int) Math.ceil(travelMin);

                // Duration for this place
                double durationHours = CategoryDuration.durationHours(
                    candidate.getPrimaryType(), candidate.getCategories());
                int durationMin = (int) Math.ceil(durationHours * 60);

                // Check if we have enough time left in the day
                if (arrivalMin + durationMin > dayEndMin) {
                    continue;
                }

                // Check open hours (best effort)
                if (!isLikelyOpen(candidate, date, arrivalMin, arrivalMin + durationMin)) {
                    continue;
                }

                // Schedule this activity
                Activity activity = new Activity();
                activity.setSortOrder(sortOrder++);
                activity.setDay(day);
                activity.setPlaces(List.of(candidate));

                activity.setStartTime(minutesToLocalDateTime(date, arrivalMin));
                activity.setEndTime(minutesToLocalDateTime(date, arrivalMin + durationMin));

                day.getActivities().add(activity);
                activitiesToday++;

                if (candidate.getId() != null) {
                    usedPlaceIds.add(candidate.getId());
                }
                if (dedupKey != null) {
                    usedPlaceKeys.add(dedupKey);
                }
                runningCost += placeCost;
                currentTimeMin = arrivalMin + durationMin + restAfterActivityMin;
                lastPlace = candidate;
            }

            days.add(day);
        }

        return new ScheduleResult(days, runningCost, usedPlaceIds, usedPlaceKeys);
    }

    /** Hard cap so LOW (“relaxed”) cannot pack a full window with many short stops. */
    private static int maxActivitiesPerDay(TripIntensity pace) {
        return switch (pace) {
            case LOW -> 4;
            case MEDIUM -> 8;
            case HIGH -> Integer.MAX_VALUE;
        };
    }

    /** Extra idle time after each stop before the next slot (coffee, transit slack). */
    private static int restMinutesBetweenStops(TripIntensity pace) {
        return switch (pace) {
            case LOW -> 45;
            case MEDIUM -> 18;
            case HIGH -> 0;
        };
    }

    /** Check if a place is likely open during [startMin, endMin) on the given date. */
    private boolean isLikelyOpen(Place place, LocalDate date, int startMin, int endMin) {
        if (place.getOpenHours() == null || place.getOpenHours().isEmpty()) {
            return true; // no data, assume open
        }
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        for (OpenHours oh : place.getOpenHours()) {
            if (oh.getDay() != null && oh.getDay().equalsIgnoreCase(dayName)) {
                return parseAndCheckHours(oh.getHours(), startMin, endMin);
            }
        }
        return true; // day not found in schedule, assume open
    }

    private boolean parseAndCheckHours(String hoursStr, int startMin, int endMin) {
        if (hoursStr == null || hoursStr.isBlank()) return true;
        try {
            // Handle formats like "09:00 - 17:00" or "9:00 – 17:00"
            String[] parts = hoursStr.split("[\\-–]");
            if (parts.length != 2) return true;
            int openMin = parseTimeToMinutes(parts[0].trim());
            int closeMin = parseTimeToMinutes(parts[1].trim());
            return startMin >= openMin && endMin <= closeMin;
        } catch (Exception e) {
            return true; // can't parse, assume open
        }
    }

    private int parseTimeToMinutes(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return hours * 60 + minutes;
    }

    private LocalDateTime minutesToLocalDateTime(LocalDate baseDate, int minutes) {
        int extraDays = minutes / (24 * 60);
        int remainderMinutes = minutes % (24 * 60);
        int h = remainderMinutes / 60;
        int m = remainderMinutes % 60;
        return LocalDateTime.of(baseDate.plusDays(extraDays), LocalTime.of(h, m));
    }

    /** Build a dedup key for transient places without a persisted ID. */
    private String buildDedupKey(Place candidate) {
        if (candidate.getGooglePlaceId() != null && !candidate.getGooglePlaceId().isBlank()) {
            return "gid:" + candidate.getGooglePlaceId();
        }
        String title = candidate.getTitle();
        String address = candidate.getAddress();
        if (title != null && !title.isBlank()) {
            return "ta:" + title.strip() + "|" + (address != null ? address.strip() : "");
        }
        return null;
    }

    public record ScheduleResult(
        List<Day> days,
        double totalEstimatedCost,
        Set<Long> usedPlaceIds,
        Set<String> usedPlaceKeys) {}
}
