package com.moveinsync.distance.core.model;

/**
 * Whether a {@link DistanceResult} was produced with full routing (OK) or had to fall back
 * to degraded aerial distance because the provider was unavailable (DEGRADED).
 */
public enum ResultStatus {
    OK,
    DEGRADED
}
