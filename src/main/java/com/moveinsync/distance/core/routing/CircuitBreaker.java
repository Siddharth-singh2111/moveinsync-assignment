package com.moveinsync.distance.core.routing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A minimal circuit breaker for the routing provider. It opens after a run of consecutive
 * failures, fast-fails for a cooldown so a dying provider is left alone, then allows a single
 * probe (half-open) whose outcome either closes it again or re-opens it.
 *
 * <p>Time comes from an injected {@link Clock} so the state transitions are deterministic and
 * testable. All transitions are synchronized, so it is safe to share across threads.
 */
public final class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be > 0");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = Objects.requireNonNull(openDuration, "openDuration");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Whether a request may proceed now. Transitions OPEN to HALF_OPEN once the cooldown passes. */
    public synchronized boolean allowRequest() {
        if (state == State.OPEN) {
            if (Duration.between(openedAt, clock.instant()).compareTo(openDuration) >= 0) {
                state = State.HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }

    public synchronized void onSuccess() {
        consecutiveFailures = 0;
        state = State.CLOSED;
    }

    public synchronized void onFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAt = clock.instant();
        }
    }

    public synchronized State state() {
        return state;
    }
}
