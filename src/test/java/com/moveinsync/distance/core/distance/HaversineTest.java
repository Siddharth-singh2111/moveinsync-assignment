package com.moveinsync.distance.core.distance;

import com.moveinsync.distance.core.model.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineTest {

    @Test
    void samePointIsZero() {
        GeoPoint p = new GeoPoint(12.9716, 77.5946);
        assertThat(Haversine.meters(p, p)).isZero();
    }

    @Test
    void isSymmetric() {
        GeoPoint a = new GeoPoint(12.9716, 77.5946);
        GeoPoint b = new GeoPoint(13.0827, 80.2707);
        assertThat(Haversine.meters(a, b)).isEqualTo(Haversine.meters(b, a));
    }

    @Test
    void oneDegreeOfLatitudeIsAboutOneEleven() {
        double m = Haversine.meters(new GeoPoint(0, 0), new GeoPoint(1, 0));
        assertThat(m).isCloseTo(111_195.0, within(100.0));
    }

    @Test
    void oneDegreeOfLongitudeAtEquatorIsAboutOneEleven() {
        double m = Haversine.meters(new GeoPoint(0, 0), new GeoPoint(0, 1));
        assertThat(m).isCloseTo(111_195.0, within(100.0));
    }

    @Test
    void longitudeDistanceShrinksTowardThePoles() {
        double atEquator = Haversine.meters(new GeoPoint(0, 0), new GeoPoint(0, 1));
        double farNorth = Haversine.meters(new GeoPoint(60, 0), new GeoPoint(60, 1));
        assertThat(farNorth).isLessThan(atEquator);
    }

    @Test
    void bangaloreToChennaiStraightLineIsAboutTwoNinety() {
        double m = Haversine.meters(new GeoPoint(12.9716, 77.5946), new GeoPoint(13.0827, 80.2707));
        assertThat(m).isBetween(280_000.0, 300_000.0);
    }
}
