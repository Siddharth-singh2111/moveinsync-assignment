package com.moveinsync.distance.core.distance;

import com.moveinsync.distance.core.model.GeoPoint;

/**
 * Great-circle (haversine) distance between two points, in metres.
 *
 * <p>This treats the Earth as a sphere of mean radius {@value #EARTH_RADIUS_METERS} m. The
 * error against the true WGS84 ellipsoid is under ~0.5% at city scale, which is acceptable
 * here: the straight-line distance is only ever used for short hops and parked stretches, or
 * as a bounded fallback — real gaps are routed. It is always a lower bound on road distance,
 * which is why the degraded fallback multiplies it (see DESIGN.md).
 */
public final class Haversine {

    /** IUGG mean Earth radius, in metres. */
    public static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private Haversine() {
    }

    /** Great-circle distance between {@code a} and {@code b}, in metres (always >= 0). */
    public static double meters(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());

        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return EARTH_RADIUS_METERS * c;
    }
}
