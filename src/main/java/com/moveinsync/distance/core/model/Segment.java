package com.moveinsync.distance.core.model;

import java.util.Objects;

/**
 * One leg of the reconstructed trip: the distance between two consecutive timeline points,
 * the rule that produced it, and whether it cost a routing-provider call. Distance is held
 * in integer metres so sums stay exact.
 */
public record Segment(GeoPoint from, GeoPoint to, long meters, SegmentRule rule, boolean providerCall) {

    public Segment {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(rule, "rule");
        if (meters < 0) {
            throw new IllegalArgumentException("meters must be >= 0: " + meters);
        }
    }
}
