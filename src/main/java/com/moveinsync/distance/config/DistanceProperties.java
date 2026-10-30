package com.moveinsync.distance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding of the {@code distance.*} configuration. Everything the engine needs to be
 * tuned per deployment lives here, including the rule version that makes results reproducible.
 */
@ConfigurationProperties(prefix = "distance")
public record DistanceProperties(Rules rules, Cache cache, Provider provider) {

    public record Rules(int version,
                        double maxSpeedKmh,
                        double minAccuracyM,
                        double shortGapSeconds,
                        double parkedSpeedKmh,
                        double officeRadiusM,
                        double routedMaxKm,
                        double degradedMultiplier,
                        boolean includeNoShowAnchors) {
    }

    public record Cache(int snapDecimals, long maxSize, long ttlHours) {
    }

    public record Provider(double roadFactor, int circuitFailureThreshold, long circuitOpenSeconds) {
    }
}
