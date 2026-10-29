package com.moveinsync.distance.core.routing;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Test
    void allowsRequestsWhileClosed() {
        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(10), new MutableClock(START));
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void opensAfterThresholdFailures() {
        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(10), new MutableClock(START));
        breaker.onFailure();
        breaker.onFailure();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
        breaker.onFailure();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    void goesHalfOpenAfterCooldown() {
        MutableClock clock = new MutableClock(START);
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofSeconds(10), clock);
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isFalse();

        clock.advance(Duration.ofSeconds(10));
        assertThat(breaker.allowRequest()).isTrue();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    void closesOnSuccessfulProbe() {
        MutableClock clock = new MutableClock(START);
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofSeconds(10), clock);
        breaker.onFailure();
        clock.advance(Duration.ofSeconds(10));
        breaker.allowRequest(); // -> HALF_OPEN
        breaker.onSuccess();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void reopensWhenProbeFails() {
        MutableClock clock = new MutableClock(START);
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofSeconds(10), clock);
        breaker.onFailure();
        clock.advance(Duration.ofSeconds(10));
        breaker.allowRequest(); // -> HALF_OPEN
        breaker.onFailure();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
