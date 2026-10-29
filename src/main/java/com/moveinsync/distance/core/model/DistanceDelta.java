package com.moveinsync.distance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * A signed correction to a trip's distance, appended to the ledger when a recompute changes
 * a result that was already billed — e.g. late pings arrive, or the provider recovers. The
 * closed result is never mutated; the delta ({@code +1.8 km} / {@code -0.4 km}) is what
 * billing reconciles against.
 */
public record DistanceDelta(String tripId,
                            String fromRef,
                            String toRef,
                            long deltaMeters,
                            String reason,
                            Instant createdAt) {

    public DistanceDelta {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** The signed correction in kilometres (may be negative), half-up to 3 dp. */
    public BigDecimal deltaKm() {
        return BigDecimal.valueOf(deltaMeters)
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }
}
