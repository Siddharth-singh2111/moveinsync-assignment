package com.moveinsync.distance.benchmark;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkRunnerTest {

    @Test
    void smallRunCompletesAndCachingReducesCalls() {
        BenchmarkRunner.BenchmarkResult r = BenchmarkRunner.run(50, 200);

        assertThat(r.trips()).isEqualTo(50);
        assertThat(r.requestsBeforeCache()).isPositive();
        assertThat(r.requestsAfterCache()).isLessThan(r.requestsBeforeCache());
        assertThat(r.p99Millis()).isGreaterThanOrEqualTo(0.0);
    }
}
