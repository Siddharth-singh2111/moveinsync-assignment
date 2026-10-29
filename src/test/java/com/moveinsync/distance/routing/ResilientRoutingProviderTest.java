package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.CircuitBreaker;
import com.moveinsync.distance.core.routing.RoutingException;
import com.moveinsync.distance.core.routing.RoutingProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilientRoutingProviderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final GeoPoint a = new GeoPoint(12.9, 77.6);
    private final GeoPoint b = new GeoPoint(13.0, 77.7);

    @Test
    void fastFailsWhenOpenWithoutCallingDelegate() {
        AtomicInteger calls = new AtomicInteger();
        RoutingProvider failing = (from, to) -> {
            calls.incrementAndGet();
            throw new RoutingException("down");
        };
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30), Clock.fixed(NOW, ZoneOffset.UTC));
        ResilientRoutingProvider provider = new ResilientRoutingProvider(failing, breaker);

        assertThatThrownBy(() -> provider.driveMeters(a, b)).isInstanceOf(RoutingException.class);
        assertThatThrownBy(() -> provider.driveMeters(a, b)).isInstanceOf(RoutingException.class);
        // Breaker is now open; the next call must not reach the delegate.
        assertThatThrownBy(() -> provider.driveMeters(a, b)).isInstanceOf(RoutingException.class);

        assertThat(calls.get()).isEqualTo(2);
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void passesThroughAndKeepsBreakerClosedOnSuccess() {
        RoutingProvider ok = (from, to) -> 4242L;
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30), Clock.fixed(NOW, ZoneOffset.UTC));
        ResilientRoutingProvider provider = new ResilientRoutingProvider(ok, breaker);

        assertThat(provider.driveMeters(a, b)).isEqualTo(4242L);
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
