package com.travel.explorer.service.scheduling;

/**
 * Haversine distance calculation and travel time estimation.
 */
public final class HaversineUtil {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CITY_SPEED_KMH = 30.0;

    private HaversineUtil() {}

    /** Distance in km between two lat/lng points. */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** Estimated travel time in minutes at city speed (~30 km/h). */
    public static double travelTimeMinutes(double lat1, double lng1, double lat2, double lng2) {
        double km = distanceKm(lat1, lng1, lat2, lng2);
        return (km / CITY_SPEED_KMH) * 60.0;
    }
}
