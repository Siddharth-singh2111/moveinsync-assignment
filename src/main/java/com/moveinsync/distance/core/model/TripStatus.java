package com.moveinsync.distance.core.model;

/**
 * Lifecycle of a trip's billing. A trip is COMPUTED once a distance has been produced, and
 * BILLED once its invoice is closed — after which the result is never mutated, only corrected
 * by appended deltas.
 */
public enum TripStatus {
    OPEN,
    COMPUTED,
    BILLED
}
