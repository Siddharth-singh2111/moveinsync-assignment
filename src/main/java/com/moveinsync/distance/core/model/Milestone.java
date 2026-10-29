package com.moveinsync.distance.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A trip event: the trip start, or an employee pickup / drop / no-show. The coordinates
 * and time are optional because they are not always captured; when they are missing the
 * anchor builder falls back to the rider's profile or planned data.
 */
public record Milestone(MilestoneKind kind, String riderId, GeoPoint location, Instant time) {

    public Milestone {
        Objects.requireNonNull(kind, "kind");
    }
}
