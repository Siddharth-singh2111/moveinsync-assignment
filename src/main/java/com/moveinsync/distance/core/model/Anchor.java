package com.moveinsync.distance.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A trusted point added to the trip timeline to patch a gap — an employee pickup/drop, a
 * no-show location, or the office. Unlike a raw ping, an anchor is always valid and always
 * carries a time, so it can be placed on the timeline.
 */
public record Anchor(AnchorSource source, GeoPoint location, Instant time) {

    public Anchor {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(time, "time");
    }
}
