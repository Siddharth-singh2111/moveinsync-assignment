package com.moveinsync.distance.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single place that converts a canonical integer-metre distance into billable
 * kilometres. Distances are summed as {@code long} metres and converted here exactly once,
 * half-up to three decimals, so two runs of the same inputs can never disagree by rounding.
 */
public final class Metres {

    private static final BigDecimal METRES_PER_KM = BigDecimal.valueOf(1000);

    private Metres() {
    }

    /** Convert metres (may be negative, for a delta) to kilometres, half-up to 3 dp. */
    public static BigDecimal toKilometres(long meters) {
        return BigDecimal.valueOf(meters).divide(METRES_PER_KM, 3, RoundingMode.HALF_UP);
    }
}
