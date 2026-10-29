package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.util.Optional;

/**
 * The terminal rule: a real gap is routed through the provider. The returned driving distance
 * is sanity-checked — rejected if it exceeds the per-segment cap, or if its implied speed is
 * over the limit <i>and</i> even a conservative straight-line x1.4 would be too fast. A rejected
 * value falls back to the straight-line distance rather than trusting an implausible route.
 */
final class RoutedClassifier implements SegmentClassifier {

    private final RoutingProvider provider;

    RoutedClassifier(RoutingProvider provider) {
        this.provider = provider;
    }

    @Override
    public Optional<Segment> classify(PairContext c) {
        long routed = provider.driveMeters(c.from().location(), c.to().location());

        double routedSpeedKmh = (routed / c.gapSeconds()) * 3.6;
        double aerialBoundSpeedKmh = (c.aerialMeters() * 1.4 / c.gapSeconds()) * 3.6;
        boolean tooFar = routed > c.config().routedMaxMeters();
        boolean implausibleSpeed = routedSpeedKmh > c.config().maxSpeedKmh()
                && aerialBoundSpeedKmh > c.config().maxSpeedKmh();

        if (tooFar || implausibleSpeed) {
            return Optional.of(new Segment(c.from().location(), c.to().location(),
                    c.aerialMeters(), SegmentRule.REJECTED_SPEED, true));
        }
        return Optional.of(new Segment(c.from().location(), c.to().location(),
                routed, SegmentRule.ROUTED, true));
    }
}
