package com.moveinsync.distance.api;

import com.moveinsync.distance.api.dto.Dtos;
import com.moveinsync.distance.service.TripComputation;
import com.moveinsync.distance.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface over {@link TripService}. The tenant is taken from the {@code X-Tenant-Id} header
 * for now; once auth is in place it comes from the authenticated principal instead, so it cannot
 * be spoofed by a caller.
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @PostMapping
    public Dtos.DistanceResponse compute(@RequestHeader("X-Tenant-Id") String tenantId,
                                         @Valid @RequestBody Dtos.ComputeTripRequest request) {
        TripComputation out = service.compute(tenantId, ApiMapper.toInputs(request));
        return ApiMapper.toDistanceResponse(out.result(), out.appendedDelta());
    }

    @GetMapping("/{tripId}/distance")
    public Dtos.DistanceResponse distance(@RequestHeader("X-Tenant-Id") String tenantId,
                                          @PathVariable String tripId) {
        return ApiMapper.toDistanceResponse(service.result(tenantId, tripId), null);
    }

    @GetMapping("/{tripId}/explain")
    public Dtos.ExplainResponse explain(@RequestHeader("X-Tenant-Id") String tenantId,
                                        @PathVariable String tripId) {
        return ApiMapper.toExplainResponse(service.result(tenantId, tripId));
    }

    @PostMapping("/{tripId}/pings")
    public Dtos.DistanceResponse ingestPings(@RequestHeader("X-Tenant-Id") String tenantId,
                                             @PathVariable String tripId,
                                             @RequestBody Dtos.IngestPingsRequest request) {
        TripComputation out = service.ingestPings(tenantId, tripId, ApiMapper.toPings(request));
        return ApiMapper.toDistanceResponse(out.result(), out.appendedDelta());
    }

    @PostMapping("/{tripId}/recompute")
    public Dtos.DistanceResponse recompute(@RequestHeader("X-Tenant-Id") String tenantId,
                                           @PathVariable String tripId) {
        TripComputation out = service.recompute(tenantId, tripId, "manual-recompute");
        return ApiMapper.toDistanceResponse(out.result(), out.appendedDelta());
    }

    @PostMapping("/{tripId}/bill")
    public ResponseEntity<Void> bill(@RequestHeader("X-Tenant-Id") String tenantId,
                                     @PathVariable String tripId) {
        service.bill(tenantId, tripId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}/deltas")
    public Dtos.DeltasResponse deltas(@RequestHeader("X-Tenant-Id") String tenantId,
                                      @PathVariable String tripId) {
        return ApiMapper.toDeltasResponse(tripId, service.ledger(tenantId, tripId));
    }
}
