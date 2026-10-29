/**
 * Orchestration: runs the engine for a trip, enforces idempotent recompute keyed by
 * (tripId, inputHash, ruleVersion), and appends signed deltas instead of mutating a billed result.
 */
package com.moveinsync.distance.service;
