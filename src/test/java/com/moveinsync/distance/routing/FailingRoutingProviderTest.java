package com.moveinsync.distance.routing;

import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.routing.RoutingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailingRoutingProviderTest {

    @Test
    void alwaysThrowsRoutingException() {
        FailingRoutingProvider provider = new FailingRoutingProvider();
        assertThatThrownBy(() -> provider.driveMeters(new GeoPoint(0, 0), new GeoPoint(1, 1)))
                .isInstanceOf(RoutingException.class);
    }
}
