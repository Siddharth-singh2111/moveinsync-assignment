package com.moveinsync.distance.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MetresTest {

    @Test
    void convertsMetresToKilometres() {
        assertThat(Metres.toKilometres(30_600)).isEqualByComparingTo(new BigDecimal("30.600"));
    }

    @Test
    void keepsMillimetreScale() {
        assertThat(Metres.toKilometres(1)).isEqualByComparingTo(new BigDecimal("0.001"));
    }

    @Test
    void handlesNegativeDelta() {
        assertThat(Metres.toKilometres(-400)).isEqualByComparingTo(new BigDecimal("-0.400"));
    }
}
