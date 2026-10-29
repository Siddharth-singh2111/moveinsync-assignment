package com.moveinsync.distance.metrics;

import com.moveinsync.distance.core.model.DistanceResult;
import com.moveinsync.distance.core.model.ResultStatus;
import com.moveinsync.distance.core.model.Segment;
import com.moveinsync.distance.core.model.SegmentRule;
import com.moveinsync.distance.routing.CachingRoutingProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * The counters, gauges and timer an on-call engineer needs: how many trips computed and how many
 * degraded, how many routed segments were rejected, how many signed deltas were appended, the
 * compute latency (p50/p99), and the live provider-call / cache-hit numbers.
 */
@Component
public class DistanceMetrics {

    private final Counter tripsComputed;
    private final Counter tripsDegraded;
    private final Counter segmentsRejected;
    private final Counter recomputeDeltas;
    private final Timer computeLatency;

    public DistanceMetrics(MeterRegistry registry, CachingRoutingProvider routingProvider) {
        this.tripsComputed = Counter.builder("distance.trips.computed")
                .description("trips computed").register(registry);
        this.tripsDegraded = Counter.builder("distance.trips.degraded")
                .description("trips that fell back to degraded distance").register(registry);
        this.segmentsRejected = Counter.builder("distance.segments.rejected")
                .description("routed segments rejected by the sanity limits").register(registry);
        this.recomputeDeltas = Counter.builder("distance.recompute.deltas")
                .description("signed deltas appended to billed trips").register(registry);
        this.computeLatency = Timer.builder("distance.compute.latency")
                .description("engine compute time").publishPercentiles(0.5, 0.99).register(registry);

        Gauge.builder("distance.provider.calls", routingProvider, CachingRoutingProvider::delegateCalls)
                .description("calls that reached the routing provider").register(registry);
        Gauge.builder("distance.provider.cache.hits", routingProvider, CachingRoutingProvider::cacheHits)
                .description("routing calls served from cache").register(registry);
        Gauge.builder("distance.provider.cache.size", routingProvider, CachingRoutingProvider::cacheSize)
                .description("distinct cells cached").register(registry);
    }

    /** Time the supplied compute and return its result. */
    public DistanceResult time(Supplier<DistanceResult> compute) {
        return computeLatency.record(compute);
    }

    /** Record counters derived from a finished result. */
    public void recordComputation(DistanceResult result) {
        tripsComputed.increment();
        if (result.status() == ResultStatus.DEGRADED) {
            tripsDegraded.increment();
        }
        long rejected = 0;
        for (Segment s : result.segments()) {
            if (s.rule() == SegmentRule.REJECTED_SPEED) {
                rejected++;
            }
        }
        if (rejected > 0) {
            segmentsRejected.increment(rejected);
        }
    }

    public void recordDeltaAppended() {
        recomputeDeltas.increment();
    }
}
