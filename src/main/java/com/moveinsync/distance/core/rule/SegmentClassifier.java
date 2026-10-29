package com.moveinsync.distance.core.rule;

import com.moveinsync.distance.core.model.Segment;

import java.util.Optional;

/**
 * One rule in the ordered chain. Returns a labelled {@link Segment} if it applies to the pair,
 * or empty to defer to the next rule. The last rule in the chain is terminal.
 */
interface SegmentClassifier {

    Optional<Segment> classify(PairContext context);
}
