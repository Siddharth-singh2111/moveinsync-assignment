package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;

import java.util.Optional;

/**
 * A long gap over which the cab barely moved (implied speed below the parked threshold) is the
 * cab sitting still, not driving — so the straight-line distance is correct and no routing call
 * is spent.
 */
final class ParkedClassifier implements SegmentClassifier {

    @Override
    public Optional<Segment> classify(PairContext c) {
        if (c.hasPositiveGap() && c.aerialSpeedKmh() < c.config().parkedSpeedKmh()) {
            return Optional.of(new Segment(c.from().location(), c.to().location(),
                    c.aerialMeters(), SegmentRule.PARKED, false));
        }
        return Optional.empty();
    }
}
