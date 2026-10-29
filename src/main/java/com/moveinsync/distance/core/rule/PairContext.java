package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.model.TimelinePoint;

/**
 * Precomputed facts about one consecutive pair of timeline points, handed to each classifier
 * so the maths is done once. {@code aerialSpeedKmh} is meaningful only when
 * {@code hasPositiveGap} is true (a non-positive gap cannot yield a speed).
 */
record PairContext(TimelinePoint from,
                   TimelinePoint to,
                   long aerialMeters,
                   double gapSeconds,
                   boolean hasPositiveGap,
                   double aerialSpeedKmh,
                   RuleConfig config) {
}
