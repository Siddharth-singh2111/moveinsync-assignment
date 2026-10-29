package com.moveinsync.distance.service;

import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.TripStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TripServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TripService service;

    private static Ping ping(double lat, double lon, long seconds) {
        return new Ping(new GeoPoint(lat, lon), T0.plusSeconds(seconds), 10);
    }

    private static TripInputs realGapTrip(String tripId) {
        return new TripInputs(tripId,
                List.of(ping(12.9000, 77.6000, 0), ping(12.9450, 77.6000, 600)),
                List.of(), null, null, null, null);
    }

    @Test
    void computeStoresAComputedTrip() {
        TripComputation out = service.compute("tenantA", realGapTrip("t-compute"));
        assertThat(out.status()).isEqualTo(TripStatus.COMPUTED);
        assertThat(out.result().totalMeters()).isPositive();
        assertThat(service.result("tenantA", "t-compute").totalMeters()).isEqualTo(out.result().totalMeters());
    }

    @Test
    void recomputeOfBilledTripWithSameInputsAppendsNoDelta() {
        service.compute("tenantA", realGapTrip("t-idem"));
        service.bill("tenantA", "t-idem");
        TripComputation out = service.recompute("tenantA", "t-idem", "recompute");
        assertThat(out.appendedDelta()).isNull();
        assertThat(service.ledger("tenantA", "t-idem")).isEmpty();
    }

    @Test
    void latePingsAfterBillingAppendSignedDeltaAndKeepBilledTotal() {
        service.compute("tenantA", realGapTrip("t-late"));
        long billedTotal = service.result("tenantA", "t-late").totalMeters();
        service.bill("tenantA", "t-late");

        TripComputation out = service.ingestPings("tenantA", "t-late",
                List.of(ping(12.9900, 77.6000, 1200)));   // extends the trip

        assertThat(out.appendedDelta()).isNotNull();
        assertThat(out.appendedDelta().deltaMeters()).isPositive();
        assertThat(service.ledger("tenantA", "t-late")).hasSize(1);
        // The billed snapshot is unchanged; only the ledger grows.
        assertThat(service.result("tenantA", "t-late").totalMeters()).isEqualTo(billedTotal);
    }

    @Test
    void oneTenantCannotReadAnothersTrip() {
        service.compute("tenantA", realGapTrip("t-iso"));
        assertThatThrownBy(() -> service.result("tenantB", "t-iso"))
                .isInstanceOf(TripNotFoundException.class);
    }
}
