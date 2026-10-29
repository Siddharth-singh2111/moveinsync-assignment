package com.moveinsync.distance.core.distance;

import com.moveinsync.distance.core.model.GeoPoint;

/**
 * Distance helpers that turn a raw {@code double} metre value into the canonical integer
 * metres the engine sums. Keeping the rounding here means every segment distance is produced
 * the same way, whether it came from haversine or from the routing provider.
 */
public final class Distances {

    private Distances() {
    }

    /** Round a non-negative, finite metre value to the nearest whole metre. */
    public static long roundMeters(double meters) {
        if (Double.isNaN(meters) || Double.isInfinite(meters)) {
            throw new IllegalArgumentException("meters must be finite: " + meters);
        }
        if (meters < 0) {
            throw new IllegalArgumentException("meters must be >= 0: " + meters);
        }
        return Math.round(meters);
    }

    /** Straight-line distance between two points, rounded to whole metres. */
    public static long aerialMeters(GeoPoint a, GeoPoint b) {
        return roundMeters(Haversine.meters(a, b));
    }
}
