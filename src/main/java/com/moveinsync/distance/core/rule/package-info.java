/**
 * The rule engine: an ordered chain of SegmentRule strategies evaluated in a fixed, versioned
 * order (short-gap aerial, parked, routed, rejected-speed, degraded). Determinism is a property
 * of the strategy order plus the rule version, not of call timing.
 */
package com.moveinsync.distance.core.rule;
