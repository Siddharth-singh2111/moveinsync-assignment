package com.moveinsync.distance.core.clean;

import com.moveinsync.distance.core.distance.Haversine;
import com.moveinsync.distance.core.model.Ping;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Turns a raw, noisy ping stream into a clean, chronological one in four steps:
 *
 * <ol>
 *   <li><b>Order</b> — sort by timestamp, so out-of-order delivery cannot corrupt the path.</li>
 *   <li><b>Dedupe</b> — drop exact duplicate fixes (a device that sent the same ping twice).</li>
 *   <li><b>Accuracy</b> — drop fixes vaguer than the configured threshold.</li>
 *   <li><b>Teleport</b> — drop physically impossible jumps whose implied speed, measured from
 *       the last <i>kept</i> ping, exceeds the limit. Measuring from the last kept ping means a
 *       single bad fix cannot also discard the good fix that follows it.</li>
 * </ol>
 *
 * <p>Pure and stateless beyond its thresholds: the same input always yields the same output.
 */
public final class StreamCleaner {

    private final double accuracyThresholdMeters;
    private final double maxSpeedKmh;

    public StreamCleaner(double accuracyThresholdMeters, double maxSpeedKmh) {
        if (accuracyThresholdMeters < 0) {
            throw new IllegalArgumentException("accuracyThresholdMeters must be >= 0");
        }
        if (maxSpeedKmh <= 0) {
            throw new IllegalArgumentException("maxSpeedKmh must be > 0");
        }
        this.accuracyThresholdMeters = accuracyThresholdMeters;
        this.maxSpeedKmh = maxSpeedKmh;
    }

    /** Clean the raw ping stream. The input list is not modified. */
    public List<Ping> clean(List<Ping> raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.isEmpty()) {
            return List.of();
        }

        // 1. Order by timestamp (stable: equal timestamps keep their relative order).
        List<Ping> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparing(Ping::timestamp));

        // 2. Dedupe exact duplicates and 3. drop low-accuracy fixes.
        List<Ping> filtered = new ArrayList<>(sorted.size());
        Set<Ping> seen = new HashSet<>();
        for (Ping p : sorted) {
            if (p.accuracyMeters() > accuracyThresholdMeters) {
                continue;
            }
            if (seen.add(p)) {
                filtered.add(p);
            }
        }

        // 4. Drop teleport jumps, measuring implied speed from the last kept ping.
        List<Ping> result = new ArrayList<>(filtered.size());
        Ping lastKept = null;
        for (Ping p : filtered) {
            if (lastKept == null) {
                result.add(p);
                lastKept = p;
                continue;
            }
            double meters = Haversine.meters(lastKept.location(), p.location());
            double seconds = Duration.between(lastKept.timestamp(), p.timestamp()).toNanos() / 1_000_000_000.0;
            if (seconds <= 0) {
                // Same or earlier time than the last kept ping but a different place: impossible.
                continue;
            }
            double speedKmh = (meters / seconds) * 3.6;
            if (speedKmh > maxSpeedKmh) {
                continue;
            }
            result.add(p);
            lastKept = p;
        }
        return List.copyOf(result);
    }
}
