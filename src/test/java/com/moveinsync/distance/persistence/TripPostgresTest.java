package com.moveinsync.distance.persistence;

import com.moveinsync.distance.core.engine.TripInputs;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.service.TripComputation;
import com.moveinsync.distance.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the full persistence path against a real PostgreSQL (not H2), proving the JSON
 * (LONGVARCHAR) columns, enum mapping and the delta ledger behave on the production database.
 * Skips automatically when Docker is unavailable, so it never blocks a dev without Docker.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TripPostgresTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TripService service;

    private static Ping ping(double lat, double lon, long seconds) {
        return new Ping(new GeoPoint(lat, lon), T0.plusSeconds(seconds), 10);
    }

    @Test
    void computesBillsAndReconcilesOnRealPostgres() {
        service.compute("tenantA", new TripInputs("pg-1",
                List.of(ping(12.9000, 77.6000, 0), ping(12.9450, 77.6000, 600)),
                List.of(), null, null, null, null));
        long billed = service.result("tenantA", "pg-1").totalMeters();
        service.bill("tenantA", "pg-1");

        TripComputation out = service.ingestPings("tenantA", "pg-1",
                List.of(ping(12.9900, 77.6000, 1200)));

        assertThat(out.appendedDelta()).isNotNull();
        assertThat(out.appendedDelta().deltaMeters()).isPositive();
        assertThat(service.result("tenantA", "pg-1").totalMeters()).isEqualTo(billed);
        assertThat(service.ledger("tenantA", "pg-1")).hasSize(1);
    }
}
