package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.DistanceResult;

import java.time.Instant;
import java.util.List;
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

    /**
     * The correction still owed on a billed trip: the fresh total minus the billed total and every
     * delta already in the ledger. Measuring against the ledger makes this idempotent — replaying
     * the same evidence owes nothing — and stops corrections from being counted twice.
     *
     * @param ledger the trip's existing deltas, oldest first
     */
    public static Optional<DistanceDelta> sinceLedger(DistanceResult billed,
                                                      List<DistanceDelta> ledger,
                                                      DistanceResult current,
                                                      String reason,
                                                      Instant at) {
        Objects.requireNonNull(billed, "billed");
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(current, "current");
        if (!billed.tripId().equals(current.tripId())) {
            throw new IllegalArgumentException("cannot delta across different trips");
        }
        long reconciledMeters = billed.totalMeters()
                + ledger.stream().mapToLong(DistanceDelta::deltaMeters).sum();
        long deltaMeters = current.totalMeters() - reconciledMeters;
        if (deltaMeters == 0) {
            return Optional.empty();
        }
        String fromRef = ledger.isEmpty() ? ref(billed) : ledger.get(ledger.size() - 1).toRef();
        return Optional.of(new DistanceDelta(current.tripId(), fromRef, ref(current), deltaMeters, reason, at));
    }

    private static String ref(DistanceResult r) {
        return r.ruleVersion() + ":" + r.inputHash();
    }
}
