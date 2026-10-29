package com.moveinsync.distance.core.clean;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Ping;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreamCleanerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private final StreamCleaner cleaner = new StreamCleaner(50.0, 120.0);

    private static Ping ping(double lat, double lon, long secondsFromT0, double accuracy) {
        return new Ping(new GeoPoint(lat, lon), T0.plus(Duration.ofSeconds(secondsFromT0)), accuracy);
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertThat(cleaner.clean(List.of())).isEmpty();
    }

    @Test
    void sortsOutOfOrderPings() {
        Ping a = ping(0, 0.0000, 0, 10);
        Ping b = ping(0, 0.0005, 100, 10);
        Ping c = ping(0, 0.0010, 200, 10);
        List<Ping> cleaned = cleaner.clean(List.of(c, a, b));
        assertThat(cleaned).containsExactly(a, b, c);
    }

    @Test
    void removesExactDuplicates() {
        Ping a = ping(0, 0.0000, 0, 10);
        Ping dup = ping(0, 0.0000, 0, 10);
        Ping b = ping(0, 0.0005, 100, 10);
        assertThat(cleaner.clean(List.of(a, dup, b))).containsExactly(a, b);
    }

    @Test
    void dropsLowAccuracyFixesButKeepsTheThreshold() {
        Ping good = ping(0, 0.0000, 0, 50);    // exactly at threshold -> kept
        Ping vague = ping(0, 0.0005, 100, 80); // worse than 50 m -> dropped
        Ping good2 = ping(0, 0.0010, 200, 20);
        assertThat(cleaner.clean(List.of(good, vague, good2))).containsExactly(good, good2);
    }

    @Test
    void dropsTeleportJump() {
        Ping a = ping(0, 0.0000, 0, 10);
        Ping b = ping(0, 0.0010, 100, 10);       // ~111 m in 100 s -> ~4 km/h, kept
        Ping teleport = ping(0, 1.0000, 104, 10); // ~111 km in 4 s -> impossible, dropped
        List<Ping> cleaned = cleaner.clean(List.of(a, b, teleport));
        assertThat(cleaned).doesNotContain(teleport);
        assertThat(cleaned).containsExactly(a, b);
    }

    @Test
    void teleportDoesNotDiscardTheFollowingGoodFix() {
        Ping a = ping(0, 0.0000, 0, 10);
        Ping b = ping(0, 0.0010, 100, 10);         // kept (~4 km/h)
        Ping teleport = ping(0, 1.0000, 104, 10);  // dropped (~100,000 km/h from b)
        Ping good = ping(0, 0.0011, 200, 10);      // ~11 m from b over 100 s -> kept
        List<Ping> cleaned = cleaner.clean(List.of(a, b, teleport, good));
        assertThat(cleaned).containsExactly(a, b, good);
    }

    @Test
    void keepsNormalSlowSequence() {
        Ping a = ping(12.9716, 77.5946, 0, 10);
        Ping b = ping(12.9720, 77.5950, 60, 10);
        Ping c = ping(12.9725, 77.5955, 120, 10);
        assertThat(cleaner.clean(List.of(a, b, c))).containsExactly(a, b, c);
    }
}
