package com.moveinsync.distance.service;

import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.TripStatus;
import com.moveinsync.distance.core.routing.CircuitBreaker;
import com.moveinsync.distance.persistence.TripEntity;
import com.moveinsync.distance.persistence.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Automatically recomputes degraded trips once the routing provider has recovered. The case study
 * requires a trip billed on a degraded (provider-down) estimate to be recomputed <i>automatically</i>
 * when the provider comes back — this job is that mechanism.
 *
 * <p>It only runs while the circuit is not open (i.e., the provider is believed healthy) and only
 * touches un-billed degraded trips; billed trips are reconciled on demand so their immutable
 * snapshot is never reprocessed in a loop.
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

    /** Recompute every un-billed degraded trip; returns how many came back OK. */
    public int recoverDegradedTrips() {
        List<TripEntity> degraded =
                trips.findByStatusNotAndResultStatus(TripStatus.BILLED, ResultStatus.DEGRADED);
        int recovered = 0;
        for (TripEntity e : degraded) {
            try {
                TripComputation out = service.recompute(e.getTenantId(), e.getTripId(), "provider-recovered");
                if (out.result().status() == ResultStatus.OK) {
                    recovered++;
                }
            } catch (RuntimeException ex) {
                log.warn("auto-recovery failed for trip {}", e.getTripId(), ex);
            }
        }
        return recovered;
    }
}
