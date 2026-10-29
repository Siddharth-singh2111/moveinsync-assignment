package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;

import java.util.Optional;

/**
 * Short gaps at a plausible speed use the straight-line distance. A pair with no positive time
 * gap (two points stamped at the same instant) is also handled here, since no speed can be
 * computed and routing is not meaningful.
 */
final class ShortGapAerialClassifier implements SegmentClassifier {

    @Override
    public Optional<Segment> classify(PairContext c) {
        boolean shortAndPlausible = c.hasPositiveGap()
                && c.gapSeconds() <= c.config().shortGapSeconds()
                && c.aerialSpeedKmh() <= c.config().maxSpeedKmh();
        if (!c.hasPositiveGap() || shortAndPlausible) {
            return Optional.of(new Segment(c.from().location(), c.to().location(),
                    c.aerialMeters(), SegmentRule.SHORT_GAP_AERIAL, false));
        }
        return Optional.empty();
    }
}
