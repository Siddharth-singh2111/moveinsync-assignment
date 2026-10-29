package com.moveinsync.distance.core.model;

/**
 * Whether a rider was booked ahead of time (PLANNED) or added on the day (AD_HOC).
 * This decides the order of fallback evidence used to anchor them.
 */
public enum RiderKind {
    AD_HOC,
    PLANNED
}
