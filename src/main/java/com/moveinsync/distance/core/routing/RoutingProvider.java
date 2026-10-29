package com.moveinsync.distance.core.routing;

import com.moveinsync.distance.core.model.GeoPoint;

/**
 * The port the engine depends on for driving distance across a real gap. Implementations may
 * be slow, rate-limited or paid; the caching and circuit-breaker adapters wrap this same
 * interface. A call that cannot be served throws {@link RoutingException}.
 */
public interface RoutingProvider {

    /** Driving distance in metres between two points. Throws {@link RoutingException} on failure. */
    long driveMeters(GeoPoint from, GeoPoint to);
}
