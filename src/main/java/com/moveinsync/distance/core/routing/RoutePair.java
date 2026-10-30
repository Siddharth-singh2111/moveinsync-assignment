package com.moveinsync.distance.core.routing;

import com.moveinsync.distance.core.model.GeoPoint;

/** A from/to pair to route, used for batched routing requests. */
public record RoutePair(GeoPoint from, GeoPoint to) {
}
