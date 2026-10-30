package com.moveinsync.distance.config;

import com.moveinsync.distance.core.anchor.AnchorBuilder;
import com.moveinsync.distance.core.clean.StreamCleaner;
import com.moveinsync.distance.core.engine.DistanceEngine;
import com.moveinsync.distance.core.routing.CircuitBreaker;
import com.moveinsync.distance.core.routing.RoutingProvider;
import com.moveinsync.distance.core.rule.RuleConfig;
import com.moveinsync.distance.core.rule.RuleEngine;
import com.moveinsync.distance.routing.CachingRoutingProvider;
import com.moveinsync.distance.routing.FakeRoutingProvider;
import com.moveinsync.distance.routing.ResilientRoutingProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Builds the framework-free engine from configuration and exposes it as Spring beans. The
 * routing provider is layered deliberately: a deterministic base provider, wrapped by the
 * circuit breaker, wrapped by the cache — so the cache sees breaker fast-fails as misses and
 * the breaker never counts a cache hit as a provider call.
 */
@Configuration
@EnableConfigurationProperties(DistanceProperties.class)
public class EngineConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CircuitBreaker routingCircuitBreaker(DistanceProperties props, Clock clock) {
        return new CircuitBreaker(
                props.provider().circuitFailureThreshold(),
                Duration.ofSeconds(props.provider().circuitOpenSeconds()),
                clock);
    }

    @Bean
    public CachingRoutingProvider routingProvider(DistanceProperties props, CircuitBreaker breaker) {
        RoutingProvider base = new FakeRoutingProvider(props.provider().roadFactor());
        RoutingProvider resilient = new ResilientRoutingProvider(base, breaker);
        return new CachingRoutingProvider(resilient, props.cache().snapDecimals(),
                props.cache().maxSize(), Duration.ofHours(props.cache().ttlHours()));
    }

    @Bean
    public StreamCleaner streamCleaner(DistanceProperties props) {
        return new StreamCleaner(props.rules().minAccuracyM(), props.rules().maxSpeedKmh());
    }

    @Bean
    public AnchorBuilder anchorBuilder(DistanceProperties props) {
        return new AnchorBuilder(props.rules().includeNoShowAnchors(), props.rules().officeRadiusM());
    }

    @Bean
    public RuleEngine ruleEngine(CachingRoutingProvider routingProvider, DistanceProperties props) {
        RuleConfig config = new RuleConfig(
                props.rules().version(),
                props.rules().maxSpeedKmh(),
                props.rules().shortGapSeconds(),
                props.rules().parkedSpeedKmh(),
                props.rules().routedMaxKm() * 1000.0,
                props.rules().degradedMultiplier());
        return new RuleEngine(routingProvider, config);
    }

    @Bean
    public DistanceEngine distanceEngine(StreamCleaner cleaner,
                                         AnchorBuilder anchorBuilder,
                                         RuleEngine ruleEngine,
                                         Clock clock) {
        return new DistanceEngine(cleaner, anchorBuilder, ruleEngine, clock);
    }
}
