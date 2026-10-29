package com.moveinsync.distance.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A raw GPS fix from the device, exactly as received. Pings are noisy: they may be
 * duplicated, out of order, inaccurate, or physically impossible — the cleaning stage
 * deals with that.
 */
public record Ping(GeoPoint location, Instant timestamp, double accuracyMeters) {

    public Ping {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(timestamp, "timestamp");
        if (accuracyMeters < 0) {
            throw new IllegalArgumentException("accuracyMeters must be >= 0: " + accuracyMeters);
        }
    }
}
