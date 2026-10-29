package com.moveinsync.distance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the whole application context (engine beans, persistence, security, metrics, web)
 * wires up and starts. This is the same context {@code docker compose up} boots, against Postgres.
 */
@SpringBootTest
@ActiveProfiles("test")
class DistanceApplicationTests {

    @Test
    void contextLoads() {
    }
}
