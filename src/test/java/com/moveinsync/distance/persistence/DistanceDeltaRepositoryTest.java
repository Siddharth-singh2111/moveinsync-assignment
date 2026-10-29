package com.moveinsync.distance.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DistanceDeltaRepositoryTest {

    @Autowired
    private DistanceDeltaRepository deltas;

    private static DistanceDeltaEntity delta(String tripId, String tenantId, long meters, Instant at) {
        DistanceDeltaEntity e = new DistanceDeltaEntity();
        e.setTripId(tripId);
        e.setTenantId(tenantId);
        e.setFromRef("1:a");
        e.setToRef("1:b");
        e.setDeltaMeters(meters);
        e.setReason("late pings");
        e.setCreatedAt(at);
        return e;
    }

    @Test
    void returnsLedgerForTripInOrder() {
        deltas.save(delta("t1", "tenantA", 1800, Instant.parse("2026-01-01T00:00:00Z")));
        deltas.save(delta("t1", "tenantA", -400, Instant.parse("2026-01-02T00:00:00Z")));
        deltas.save(delta("t2", "tenantA", 999, Instant.parse("2026-01-01T00:00:00Z")));

        List<DistanceDeltaEntity> ledger = deltas.findByTripIdAndTenantIdOrderByCreatedAtAscIdAsc("t1", "tenantA");

        assertThat(ledger).hasSize(2);
        assertThat(ledger.get(0).getDeltaMeters()).isEqualTo(1800);
        assertThat(ledger.get(1).getDeltaMeters()).isEqualTo(-400);
    }

    @Test
    void doesNotLeakAcrossTenants() {
        deltas.save(delta("t1", "tenantA", 1800, Instant.now()));
        assertThat(deltas.findByTripIdAndTenantIdOrderByCreatedAtAscIdAsc("t1", "tenantB")).isEmpty();
    }
}
