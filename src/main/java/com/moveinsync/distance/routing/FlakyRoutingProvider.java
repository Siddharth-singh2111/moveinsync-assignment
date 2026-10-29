package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingException;
import com.moveinsync.distance.core.routing.RoutingProvider;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fails its first {@code failuresBeforeRecovery} calls, then delegates normally. Models a provider
 * that goes down partway through a batch and later recovers — used for failure-injection tests.
 */
public final class FlakyRoutingProvider implements RoutingProvider {

    private final RoutingProvider delegate;
    private final int failuresBeforeRecovery;
    private final AtomicInteger calls = new AtomicInteger();

    public FlakyRoutingProvider(RoutingProvider delegate, int failuresBeforeRecovery) {
        if (failuresBeforeRecovery < 0) {
            throw new IllegalArgumentException("failuresBeforeRecovery must be >= 0");
        }
        this.delegate = delegate;
        this.failuresBeforeRecovery = failuresBeforeRecovery;
    }

    @Override
    public long driveMeters(GeoPoint from, GeoPoint to) {
        if (calls.incrementAndGet() <= failuresBeforeRecovery) {
            throw new RoutingException("flaky provider failure");
        }
        return delegate.driveMeters(from, to);
    }
}
