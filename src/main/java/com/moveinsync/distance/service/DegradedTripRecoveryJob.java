package com.moveinsync.distance.service;

import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.routing.CircuitBreaker;
import com.moveinsync.distance.persistence.TripEntity;
import com.moveinsync.distance.persistence.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Automatically recomputes degraded trips once the routing provider has recovered. An un-billed trip
 * is simply updated; a billed trip keeps its invoice and gets a signed delta for the difference.
 *
 * <p>It only runs while the circuit is not open (i.e., the provider is believed healthy). It selects
 * trips by their <i>latest</i> compute status, so a billed trip is picked up once and then left alone,
 * even though its frozen result still reads DEGRADED.
 */
@Component
public class DegradedTripRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(DegradedTripRecoveryJob.class);

    private final CircuitBreaker breaker;
    private final TripRepository trips;
    private final TripService service;

    public DegradedTripRecoveryJob(CircuitBreaker breaker, TripRepository trips, TripService service) {
        this.breaker = breaker;
        this.trips = trips;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${distance.recovery.interval-ms:30000}")
    public void onSchedule() {
        if (breaker.state() == CircuitBreaker.State.OPEN) {
            return; // provider still down; nothing to gain by recomputing now
        }
        int recovered = recoverDegradedTrips();
        if (recovered > 0) {
            log.info("auto-recovery recomputed {} degraded trip(s)", recovered);
        }
    }

    /** Recompute every trip whose latest compute was degraded; returns how many came back OK. */
    public int recoverDegradedTrips() {
        List<TripEntity> degraded = trips.findByLatestResultStatus(ResultStatus.DEGRADED);
        int recovered = 0;
        for (TripEntity e : degraded) {
            try {
                TripComputation out = service.recompute(e.getTenantId(), e.getTripId(), "provider-recovered");
                if (out.computedStatus() == ResultStatus.OK) {
                    recovered++;
                }
            } catch (RuntimeException ex) {
                log.warn("auto-recovery failed for trip {}", e.getTripId(), ex);
            }
        }
        return recovered;
    }
}
