package com.orbitaldelta.gpscompass;

import java.util.Locale;

/** Pure GPS compass and coordinate rules, with no sensor or Android service dependency. */
public final class CompassLogic {
    public static final float MIN_SPEED_MPS = 1.0f;
    public static final long FRESH_FOR_MS = 15_000L;
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private CompassLogic() { }

    public static boolean isValidCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    public static String formatLatitude(double latitude) {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) return "--";
        return String.format(Locale.US, "%.6f", latitude);
    }

    public static String formatLongitude(double longitude) {
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) return "--";
        return String.format(Locale.US, "%.6f", longitude);
    }

    public static boolean isValidAccuracy(float accuracyMeters) {
        return Float.isFinite(accuracyMeters) && accuracyMeters >= 0.0f;
    }

    public static String formatAccuracy(float accuracyMeters) {
        return isValidAccuracy(accuracyMeters)
                ? String.format(Locale.US, "±%.1f m", accuracyMeters)
                : "Precisão indisponível";
    }

    public static boolean isFresh(long nowElapsedNanos, long fixElapsedNanos) {
        long ageNanos = nowElapsedNanos - fixElapsedNanos;
        return fixElapsedNanos > 0L && ageNanos >= 0L && ageNanos <= FRESH_FOR_MS * 1_000_000L;
    }

    public static boolean isValidBearing(float bearingDegrees) {
        return Float.isFinite(bearingDegrees) && bearingDegrees >= 0.0f && bearingDegrees < 360.0f;
    }

    public static boolean hasSufficientMovement(boolean hasSpeed, float speedMps) {
        return hasSpeed && Float.isFinite(speedMps) && speedMps >= MIN_SPEED_MPS;
    }

    public static int roundedBearing(float bearingDegrees) {
        if (!isValidBearing(bearingDegrees)) return -1;
        return Math.round(bearingDegrees) % 360;
    }

    public static String cardinal(float bearingDegrees) {
        if (!isValidBearing(bearingDegrees)) return "--";
        return CARDINALS[Math.round(bearingDegrees / 45.0f) % CARDINALS.length];
    }
}
