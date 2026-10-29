package com.moveinsync.distance.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One point on the reconstructed trip timeline: a location at a time, tagged with where it
 * came from. A {@code null} {@link #anchorSource()} means a raw GPS ping; a non-null value
 * means a trusted anchor the builder inserted to patch a gap.
 */
public record TimelinePoint(GeoPoint location, Instant time, AnchorSource anchorSource) {

    public TimelinePoint {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(time, "time");
    }

    /** True if this point is a trusted anchor rather than a raw ping. */
    public boolean isAnchor() {
        return anchorSource != null;
    }
}
