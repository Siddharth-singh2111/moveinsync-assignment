package com.moveinsync.distance.service;

import com.moveinsync.distance.core.model.DistanceDelta;
import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.TripStatus;

/**
 * The outcome of a compute or recompute: the authoritative result (the billed snapshot when the
 * trip is already billed), the signed delta that was appended if the recompute changed a billed
 * trip (null otherwise), the trip's current status, and how this compute itself went — which, for a
 * billed trip, can differ from the frozen result.
 */
public record TripComputation(DistanceResult result, DistanceDelta appendedDelta, TripStatus status,
                              ResultStatus computedStatus) {
}
