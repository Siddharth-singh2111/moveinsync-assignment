package com.moveinsync.distance.core.distance;

import com.moveinsync.distance.core.model.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DistancesTest {

    @Test
    void roundsToNearestMetre() {
        assertThat(Distances.roundMeters(1234.4)).isEqualTo(1234L);
        assertThat(Distances.roundMeters(1234.6)).isEqualTo(1235L);
    }

    @Test
    void roundsHalfUp() {
        assertThat(Distances.roundMeters(0.5)).isEqualTo(1L);
    }

    @Test
    void rejectsNegativeDistance() {
        assertThatThrownBy(() -> Distances.roundMeters(-1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonFiniteDistance() {
        assertThatThrownBy(() -> Distances.roundMeters(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Distances.roundMeters(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aerialMetersIsRoundedHaversine() {
        GeoPoint a = new GeoPoint(12.9716, 77.5946);
        GeoPoint b = new GeoPoint(13.0827, 80.2707);
        assertThat(Distances.aerialMeters(a, b))
                .isEqualTo(Distances.roundMeters(Haversine.meters(a, b)));
    }
}
