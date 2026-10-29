/**
 * The engine orchestrator: runs clean -> anchor -> segment -> aggregate for a trip and produces
 * a reproducible {@link com.moveinsync.distance.core.model.DistanceResult}, plus the signed-delta
 * logic used when a recompute changes an already-billed trip.
 */
package com.moveinsync.distance.core.engine;
