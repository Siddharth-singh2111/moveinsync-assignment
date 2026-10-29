package com.moveinsync.distance.core.engine;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.model.TimelinePoint;
import com.moveinsync.distance.core.rule.RuleEngine;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the full pipeline for one trip: clean the pings, build the anchored timeline,
 * classify segments, then aggregate into a {@link DistanceResult}. Pure and deterministic — the
 * only non-deterministic input, the computed-at timestamp, comes from an injected {@link Clock}.
 */
public final class DistanceEngine {

    private final StreamCleaner cleaner;
    private final AnchorBuilder anchorBuilder;
    private final RuleEngine ruleEngine;
    private final Clock clock;

    public DistanceEngine(StreamCleaner cleaner,
                          AnchorBuilder anchorBuilder,
                          RuleEngine ruleEngine,
                          Clock clock) {
        this.cleaner = cleaner;
        this.anchorBuilder = anchorBuilder;
        this.ruleEngine = ruleEngine;
        this.clock = clock;
    }

    public DistanceResult compute(TripInputs in) {
        List<Ping> cleaned = cleaner.clean(in.pings());
        List<TimelinePoint> timeline = anchorBuilder.build(
                cleaned, in.milestones(), in.ridersById(),
                in.plannedOffice(), in.actualTripEnd(), in.tripEndTime());
        List<Segment> segments = ruleEngine.segment(timeline);

        long totalMeters = 0;
        int providerCalls = 0;
        boolean degraded = false;
        for (Segment s : segments) {
            totalMeters += s.meters();
            if (s.providerCall()) {
                providerCalls++;
            }
            if (s.rule() == SegmentRule.DEGRADED_AERIAL) {
                degraded = true;
            }
        }

        ResultStatus status = degraded ? ResultStatus.DEGRADED : ResultStatus.OK;
        String inputHash = InputHasher.hash(in, ruleEngine.ruleVersion());
        return new DistanceResult(in.tripId(), totalMeters, segments, providerCalls,
                status, ruleEngine.ruleVersion(), inputHash, Instant.now(clock));
    }
}
