package com.moveinsync.distance.core.model;

/**
 * The rule that produced a {@link Segment}'s distance. Every segment carries one, so a
 * trip's total can be explained — and disputed — line by line.
 */
public enum SegmentRule {
    /** Short gap at a plausible speed: straight-line (haversine) distance. */
    SHORT_GAP_AERIAL,
    /** Long gap but the cab was effectively stationary: straight-line distance. */
    PARKED,
    /** A real gap: driving distance from the routing provider. */
    ROUTED,
    /** A routed/implied distance that failed the sanity limits; bounded aerial fallback. */
    REJECTED_SPEED,
    /** Provider was unavailable (circuit open): aerial distance times the degraded multiplier. */
    DEGRADED_AERIAL
}
