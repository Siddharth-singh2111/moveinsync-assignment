package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.distance.Distances;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.model.TimelinePoint;
import com.moveinsync.distance.core.routing.RoutePair;
import com.moveinsync.distance.core.routing.RoutingException;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a time-ordered timeline into labelled distance segments in two phases:
 *
 * <ol>
 *   <li>decide each pair's rule as far as possible without routing — short gaps and parked
 *       stretches are settled here, and the remaining "real gaps" are collected;</li>
 *   <li>route all real gaps in <b>one batched call</b> (which the caching adapter dedupes to a
 *       single round-trip), then build the routed/rejected/degraded segments from the results.</li>
 * </ol>
 *
 * The rule order is fixed, so two runs of the same timeline always produce the same segments.
 * If the batched routing call fails (provider down / circuit open), every real gap degrades to a
 * bounded aerial estimate and the trip is marked degraded.
 */
public final class RuleEngine {

    private final RoutingProvider provider;
    private final RuleConfig config;
    private final ShortGapAerialClassifier shortGap = new ShortGapAerialClassifier();
    private final ParkedClassifier parked = new ParkedClassifier();

    public RuleEngine(RoutingProvider provider, RuleConfig config) {
        this.provider = provider;
        this.config = config;
    }

    public int ruleVersion() {
        return config.version();
    }

    public List<Segment> segment(List<TimelinePoint> timeline) {
        if (timeline.size() < 2) {
            return List.of();
        }
        int n = timeline.size() - 1;

        // Phase 1: classify the cheap cases and collect the real gaps to route.
        PairContext[] contexts = new PairContext[n];
        Segment[] settled = new Segment[n];
        RoutePair[] gapAt = new RoutePair[n];
        List<RoutePair> gaps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            PairContext ctx = context(timeline.get(i), timeline.get(i + 1));
            contexts[i] = ctx;
            Segment cheap = shortGap.classify(ctx)
                    .or(() -> parked.classify(ctx))
                    .orElse(null);
            if (cheap != null) {
                settled[i] = cheap;
            } else {
                gapAt[i] = new RoutePair(ctx.from().location(), ctx.to().location());
                gaps.add(gapAt[i]);
            }
        }

        // Phase 2: one batched routing call for every real gap.
        Map<RoutePair, Long> routed = new HashMap<>();
        boolean batchFailed = false;
        if (!gaps.isEmpty()) {
            try {
                long[] results = provider.driveMeters(gaps);
                for (int k = 0; k < gaps.size(); k++) {
                    routed.put(gaps.get(k), results[k]);
                }
            } catch (RoutingException e) {
                batchFailed = true;
            }
        }

        // Phase 3: assemble.
        List<Segment> segments = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (settled[i] != null) {
                segments.add(settled[i]);
            } else {
                Long routedMeters = batchFailed ? null : routed.get(gapAt[i]);
                segments.add(routedSegment(contexts[i], routedMeters));
            }
        }
        return List.copyOf(segments);
    }

    private Segment routedSegment(PairContext c, Long routedMeters) {
        if (routedMeters == null) {
            // Provider unavailable: bill a bounded aerial estimate and mark the segment degraded.
            long degraded = Math.round(c.aerialMeters() * c.config().degradedMultiplier());
            return new Segment(c.from().location(), c.to().location(),
                    degraded, SegmentRule.DEGRADED_AERIAL, false);
        }
        long routed = routedMeters;
        double routedSpeedKmh = (routed / c.gapSeconds()) * 3.6;
        double aerialBoundSpeedKmh = (c.aerialMeters() * 1.4 / c.gapSeconds()) * 3.6;
        boolean tooFar = routed > c.config().routedMaxMeters();
        boolean implausibleSpeed = routedSpeedKmh > c.config().maxSpeedKmh()
                && aerialBoundSpeedKmh > c.config().maxSpeedKmh();

        if (tooFar || implausibleSpeed) {
            return new Segment(c.from().location(), c.to().location(),
                    c.aerialMeters(), SegmentRule.REJECTED_SPEED, true);
        }
        return new Segment(c.from().location(), c.to().location(),
                routed, SegmentRule.ROUTED, true);
    }

    private PairContext context(TimelinePoint a, TimelinePoint b) {
        long aerialMeters = Distances.aerialMeters(a.location(), b.location());
        double gapSeconds = Duration.between(a.time(), b.time()).toNanos() / 1_000_000_000.0;
        boolean hasPositiveGap = gapSeconds > 0;
        double aerialSpeedKmh = hasPositiveGap ? (aerialMeters / gapSeconds) * 3.6 : 0.0;
        return new PairContext(a, b, aerialMeters, gapSeconds, hasPositiveGap, aerialSpeedKmh, config);
    }
}
