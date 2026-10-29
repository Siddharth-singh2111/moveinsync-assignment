package com.moveinsync.distance.core.routing;

/**
 * Signals that a routing provider could not serve a request (timeout, rate limit, outage).
 * The engine treats this as the trigger for its degraded fallback rather than failing the trip.
 */
public class RoutingException extends RuntimeException {

    public RoutingException(String message) {
        super(message);
    }

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}
