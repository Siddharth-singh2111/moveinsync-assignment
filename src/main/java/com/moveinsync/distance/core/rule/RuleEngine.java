package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.distance.Distances;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.TimelinePoint;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a time-ordered timeline into labelled distance segments by running each consecutive
 * pair through a fixed, ordered chain of classifiers. The order <b>is</b> the policy, so two
 * runs of the same timeline always produce the same segments.
 */
public final class RuleEngine {

    private final List<SegmentClassifier> classifiers;
    private final RuleConfig config;

    public RuleEngine(RoutingProvider provider, RuleConfig config) {
        this.config = config;
        this.classifiers = List.of(
                new ShortGapAerialClassifier(),
                new ParkedClassifier(),
                new RoutedClassifier(provider));
    }

    public int ruleVersion() {
        return config.version();
    }

    /** Classify every consecutive pair; fewer than two points means no segments. */
    public List<Segment> segment(List<TimelinePoint> timeline) {
        if (timeline.size() < 2) {
            return List.of();
        }
        List<Segment> segments = new ArrayList<>(timeline.size() - 1);
        for (int i = 0; i < timeline.size() - 1; i++) {
            segments.add(classify(context(timeline.get(i), timeline.get(i + 1))));
        }
        return List.copyOf(segments);
    }

    private Segment classify(PairContext ctx) {
        for (SegmentClassifier classifier : classifiers) {
            var result = classifier.classify(ctx);
            if (result.isPresent()) {
                return result.get();
            }
        }
        // The routed classifier is terminal, so this is unreachable in practice.
        throw new IllegalStateException("no classifier matched the pair");
    }

    private PairContext context(TimelinePoint a, TimelinePoint b) {
        long aerialMeters = Distances.aerialMeters(a.location(), b.location());
        double gapSeconds = Duration.between(a.time(), b.time()).toNanos() / 1_000_000_000.0;
        boolean hasPositiveGap = gapSeconds > 0;
        double aerialSpeedKmh = hasPositiveGap ? (aerialMeters / gapSeconds) * 3.6 : 0.0;
        return new PairContext(a, b, aerialMeters, gapSeconds, hasPositiveGap, aerialSpeedKmh, config);
    }
}
