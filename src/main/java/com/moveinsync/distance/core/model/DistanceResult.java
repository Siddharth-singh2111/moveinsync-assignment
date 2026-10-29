package com.moveinsync.distance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The reconstructed, explainable distance for one trip. {@code totalMeters} is the canonical
 * value (integer metres); kilometres are derived once with an explicit rounding rule.
 *
 * <p>{@code inputHash} and {@code ruleVersion} together make a result reproducible: the same
 * inputs under the same rule version always yield the same kilometres.
 */
public record DistanceResult(String tripId,
                             long totalMeters,
                             List<Segment> segments,
                             int providerCalls,
                             ResultStatus status,
                             int ruleVersion,
                             String inputHash,
                             Instant computedAt) {

    public DistanceResult {
        Objects.requireNonNull(tripId, "tripId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(computedAt, "computedAt");
        segments = segments == null ? List.of() : List.copyOf(segments);
        if (totalMeters < 0) {
            throw new IllegalArgumentException("totalMeters must be >= 0: " + totalMeters);
        }
        if (providerCalls < 0) {
            throw new IllegalArgumentException("providerCalls must be >= 0: " + providerCalls);
        }
    }

    /** Billable distance in kilometres: integer metres converted once, half-up to 3 dp. */
    public BigDecimal totalKm() {
        return BigDecimal.valueOf(totalMeters)
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }
}
