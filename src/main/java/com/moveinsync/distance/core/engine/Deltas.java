package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.DistanceResult;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Compares a previous distance result with a freshly recomputed one and, when they differ,
 * produces a signed correction.
 *
 * <p>Recompute is idempotent: because {@link DistanceEngine} is deterministic, the same inputs
 * under the same rule version always give the same total, so {@link #between} returns empty and
 * nothing is appended. When something has genuinely changed — late pings, a provider recovery, a
 * new rule version — a billed result is never rewritten; instead a {@link DistanceDelta} is added
 * to the ledger and billing reconciles from it.
 */
public final class Deltas {

    private Deltas() {
    }

    /** True if the two results carry the same rule version, input hash and total. */
    public static boolean unchanged(DistanceResult previous, DistanceResult current) {
        return previous.ruleVersion() == current.ruleVersion()
                && previous.inputHash().equals(current.inputHash())
                && previous.totalMeters() == current.totalMeters();
    }

    /** A signed delta from {@code previous} to {@code current}, or empty if the total is unchanged. */
    public static Optional<DistanceDelta> between(DistanceResult previous,
                                                  DistanceResult current,
                                                  String reason,
                                                  Instant at) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        if (!previous.tripId().equals(current.tripId())) {
            throw new IllegalArgumentException("cannot delta across different trips");
        }
        long deltaMeters = current.totalMeters() - previous.totalMeters();
        if (deltaMeters == 0) {
            return Optional.empty();
        }
        return Optional.of(new DistanceDelta(current.tripId(),
                ref(previous), ref(current), deltaMeters, reason, at));
    }

    private static String ref(DistanceResult r) {
        return r.ruleVersion() + ":" + r.inputHash();
    }
}
