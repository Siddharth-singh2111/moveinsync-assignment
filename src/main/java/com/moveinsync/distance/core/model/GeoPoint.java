package com.moveinsync.distance.core.model;

/**
 * A WGS84 latitude/longitude point. Validated on construction so an out-of-range
 * coordinate can never flow into the distance maths.
 */
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude out of range: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude out of range: " + longitude);
        }
    }
}
