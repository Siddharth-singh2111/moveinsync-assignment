package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.CircuitBreaker;
import com.moveinsync.distance.core.routing.RoutePair;
import com.moveinsync.distance.core.routing.RoutingException;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.util.List;

/**
 * Guards a routing provider with a {@link CircuitBreaker}. While the breaker is open the call
 * fast-fails with a {@link RoutingException} without touching the delegate; otherwise the call
 * goes through and its success or failure is reported back to the breaker.
 */
public final class ResilientRoutingProvider implements RoutingProvider {

    private final RoutingProvider delegate;
    private final CircuitBreaker breaker;

    public ResilientRoutingProvider(RoutingProvider delegate, CircuitBreaker breaker) {
        this.delegate = delegate;
        this.breaker = breaker;
    }

    @Override
    public long driveMeters(GeoPoint from, GeoPoint to) {
        if (!breaker.allowRequest()) {
            throw new RoutingException("routing circuit is open");
        }
        try {
            long value = delegate.driveMeters(from, to);
            breaker.onSuccess();
            return value;
        } catch (RoutingException e) {
            breaker.onFailure();
            throw e;
        }
    }

    @Override
    public long[] driveMeters(List<RoutePair> pairs) {
        if (!breaker.allowRequest()) {
            throw new RoutingException("routing circuit is open");
        }
        try {
            long[] values = delegate.driveMeters(pairs);
            breaker.onSuccess();
            return values;
        } catch (RoutingException e) {
            breaker.onFailure();
            throw e;
        }
    }
}
