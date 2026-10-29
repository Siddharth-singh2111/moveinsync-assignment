package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingException;
import com.moveinsync.distance.core.routing.RoutingProvider;

/**
 * A routing provider that always fails. Used for failure injection — to drive the circuit
 * breaker open and exercise the degraded-distance fallback.
 */
public final class FailingRoutingProvider implements RoutingProvider {

    @Override
    public long driveMeters(GeoPoint from, GeoPoint to) {
        throw new RoutingException("routing provider unavailable");
    }
}
