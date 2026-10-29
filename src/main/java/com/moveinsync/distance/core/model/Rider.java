package com.moveinsync.distance.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A rider on the trip plus the fallback evidence the anchor builder uses when the rider's
 * actual pickup/drop point is missing or invalid: an ad-hoc rider's profile home and
 * sign-in time, then the planned location and planned time.
 */
public record Rider(String riderId,
                    RiderKind kind,
                    GeoPoint home,
                    Instant signInTime,
                    GeoPoint plannedLocation,
                    Instant plannedTime) {

    public Rider {
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(kind, "kind");
    }
}
