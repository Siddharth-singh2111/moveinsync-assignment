package com.moveinsync.distance.core.rule;

/**
 * The thresholds that drive segment classification, plus the rule-set version stamped onto
 * every result. Bundling them in one value object keeps the engine deterministic: a result is
 * reproducible from its inputs and this version.
 *
 * @param version          monotonically increasing rule-set version
 * @param maxSpeedKmh       speed above which a fix/route is implausible
 * @param shortGapSeconds   gaps at or below this are treated as short (straight-line)
 * @param parkedSpeedKmh    below this implied speed the cab is considered parked
 * @param routedMaxMeters   a single routed segment above this is rejected
 * @param degradedMultiplier aerial multiplier used when the provider is unavailable
 */
public record RuleConfig(int version,
                         double maxSpeedKmh,
                         double shortGapSeconds,
                         double parkedSpeedKmh,
                         double routedMaxMeters,
                         double degradedMultiplier) {

    public RuleConfig {
        if (maxSpeedKmh <= 0) {
            throw new IllegalArgumentException("maxSpeedKmh must be > 0");
        }
        if (shortGapSeconds < 0) {
            throw new IllegalArgumentException("shortGapSeconds must be >= 0");
        }
        if (parkedSpeedKmh < 0) {
            throw new IllegalArgumentException("parkedSpeedKmh must be >= 0");
        }
        if (routedMaxMeters <= 0) {
            throw new IllegalArgumentException("routedMaxMeters must be > 0");
        }
        if (degradedMultiplier < 1.0) {
            throw new IllegalArgumentException("degradedMultiplier must be >= 1.0");
        }
    }

    /** The defaults from the case study (120 km/h, 70 s, 5 km/h, 100 km, x1.4). */
    public static RuleConfig defaults() {
        return new RuleConfig(1, 120.0, 70.0, 5.0, 100_000.0, 1.4);
    }
}
