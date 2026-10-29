package com.moveinsync.distance.core.anchor;

import com.moveinsync.distance.core.model.AnchorSource;
import com.moveinsync.distance.core.model.GeoPoint;
import com.moveinsync.distance.core.model.Milestone;
import com.moveinsync.distance.core.model.MilestoneKind;
import com.moveinsync.distance.core.model.Ping;
import com.moveinsync.distance.core.model.Rider;
import com.moveinsync.distance.core.model.RiderKind;
import com.moveinsync.distance.core.model.TimelinePoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnchorBuilderTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final GeoPoint OFFICE = new GeoPoint(12.9600, 77.6400);

    private final AnchorBuilder builder = new AnchorBuilder(true, 400.0);

    private static Instant t(long seconds) {
        return T0.plusSeconds(seconds);
    }

    private List<TimelinePoint> anchorsOnly(List<TimelinePoint> points) {
        return points.stream().filter(TimelinePoint::isAnchor).toList();
    }

    @Test
    void usesActualPickupWhenValid() {
        Milestone pickup = new Milestone(MilestoneKind.PICKUP, "r1", new GeoPoint(12.90, 77.60), t(10));
        List<TimelinePoint> out = builder.build(List.of(), List.of(pickup), Map.of(), null, null, null);
        assertThat(anchorsOnly(out)).singleElement().satisfies(a -> {
            assertThat(a.anchorSource()).isEqualTo(AnchorSource.ACTUAL_PICKUP);
            assertThat(a.location()).isEqualTo(new GeoPoint(12.90, 77.60));
            assertThat(a.time()).isEqualTo(t(10));
        });
    }

    @Test
    void fallsBackToProfileHomeWhenActualMissing() {
        Milestone pickup = new Milestone(MilestoneKind.PICKUP, "r1", null, null);
        Rider r1 = new Rider("r1", RiderKind.AD_HOC, new GeoPoint(12.80, 77.50), t(5), null, null);
        List<TimelinePoint> out = builder.build(List.of(), List.of(pickup), Map.of("r1", r1), null, null, null);
        assertThat(anchorsOnly(out)).singleElement().satisfies(a -> {
            assertThat(a.anchorSource()).isEqualTo(AnchorSource.PROFILE_HOME);
            assertThat(a.location()).isEqualTo(new GeoPoint(12.80, 77.50));
            assertThat(a.time()).isEqualTo(t(5));
        });
    }

    @Test
    void fallsBackToPlannedWhenNoHome() {
        Milestone drop = new Milestone(MilestoneKind.DROP, "r2", null, null);
        Rider r2 = new Rider("r2", RiderKind.PLANNED, null, null, new GeoPoint(12.70, 77.40), t(8));
        List<TimelinePoint> out = builder.build(List.of(), List.of(drop), Map.of("r2", r2), null, null, null);
        assertThat(anchorsOnly(out)).singleElement().satisfies(a -> {
            assertThat(a.anchorSource()).isEqualTo(AnchorSource.PLANNED);
            assertThat(a.location()).isEqualTo(new GeoPoint(12.70, 77.40));
            assertThat(a.time()).isEqualTo(t(8));
        });
    }

    @Test
    void skipsEmployeeAnchorWhenNoEvidence() {
        Milestone drop = new Milestone(MilestoneKind.DROP, "r3", null, null);
        List<TimelinePoint> out = builder.build(List.of(), List.of(drop), Map.of(), null, null, null);
        assertThat(anchorsOnly(out)).isEmpty();
    }

    @Test
    void officeAnchorUsesActualEndWhenWithinRadius() {
        GeoPoint nearOffice = new GeoPoint(12.9620, 77.6400); // ~222 m north of office
        List<TimelinePoint> out = builder.build(List.of(), List.of(), Map.of(), OFFICE, nearOffice, t(100));
        assertThat(anchorsOnly(out)).singleElement().satisfies(a -> {
            assertThat(a.anchorSource()).isEqualTo(AnchorSource.ACTUAL_OFFICE);
            assertThat(a.location()).isEqualTo(nearOffice);
        });
    }

    @Test
    void officeAnchorUsesPlannedWhenBeyondRadius() {
        GeoPoint farFromOffice = new GeoPoint(12.9700, 77.6400); // ~1.1 km north of office
        List<TimelinePoint> out = builder.build(List.of(), List.of(), Map.of(), OFFICE, farFromOffice, t(100));
        assertThat(anchorsOnly(out)).singleElement().satisfies(a -> {
            assertThat(a.anchorSource()).isEqualTo(AnchorSource.PLANNED_OFFICE);
            assertThat(a.location()).isEqualTo(OFFICE);
        });
    }

    @Test
    void noShowAnchorAddedWhenEnabled() {
        Milestone noShow = new Milestone(MilestoneKind.NO_SHOW, "r4", new GeoPoint(12.50, 77.30), t(20));
        List<TimelinePoint> out = builder.build(List.of(), List.of(noShow), Map.of(), null, null, null);
        assertThat(anchorsOnly(out)).singleElement()
                .satisfies(a -> assertThat(a.anchorSource()).isEqualTo(AnchorSource.NO_SHOW));
    }

    @Test
    void noShowAnchorSkippedWhenDisabled() {
        AnchorBuilder noNoShow = new AnchorBuilder(false, 400.0);
        Milestone noShow = new Milestone(MilestoneKind.NO_SHOW, "r4", new GeoPoint(12.50, 77.30), t(20));
        List<TimelinePoint> out = noNoShow.build(List.of(), List.of(noShow), Map.of(), null, null, null);
        assertThat(anchorsOnly(out)).isEmpty();
    }

    @Test
    void timelineIsSortedByTime() {
        Ping earlyPing = new Ping(new GeoPoint(12.90, 77.60), t(0), 10);
        Ping latePing = new Ping(new GeoPoint(12.95, 77.62), t(300), 10);
        Milestone pickup = new Milestone(MilestoneKind.PICKUP, "r1", new GeoPoint(12.92, 77.61), t(150));
        List<TimelinePoint> out = builder.build(
                List.of(latePing, earlyPing), List.of(pickup), Map.of(), OFFICE, OFFICE, t(600));
        assertThat(out).isSortedAccordingTo((a, b) -> a.time().compareTo(b.time()));
        assertThat(out.get(0).time()).isEqualTo(t(0));
        assertThat(out.get(out.size() - 1).time()).isEqualTo(t(600));
    }
}
