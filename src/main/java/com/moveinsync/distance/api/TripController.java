package com.moveinsync.distance.api;

import com.moveinsync.distance.api.dto.Dtos;
import com.moveinsync.distance.security.AuthenticatedUser;
import com.moveinsync.distance.service.TripComputation;
import com.moveinsync.distance.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface over {@link TripService}. The tenant is taken from the authenticated principal
 * (the JWT), never from the request, so a caller can only ever touch their own tenant's trips.
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @PostMapping
    public Dtos.DistanceResponse compute(@AuthenticationPrincipal AuthenticatedUser caller,
                                         @Valid @RequestBody Dtos.ComputeTripRequest request) {
        TripComputation out = service.compute(caller.tenantId(), ApiMapper.toInputs(request));
        return ApiMapper.toDistanceResponse(out.result(), out.appendedDelta());
    }

    @GetMapping("/{tripId}/distance")
    public Dtos.DistanceResponse distance(@AuthenticationPrincipal AuthenticatedUser caller,
                                          @PathVariable String tripId) {
        return ApiMapper.toDistanceResponse(service.result(caller.tenantId(), tripId), null);
    }

    @GetMapping("/{tripId}/explain")
    public Dtos.ExplainResponse explain(@AuthenticationPrincipal AuthenticatedUser caller,
                                        @PathVariable String tripId) {
        return ApiMapper.toExplainResponse(service.result(caller.tenantId(), tripId));
    }

    @PostMapping("/{tripId}/pings")
    public Dtos.DistanceResponse ingestPings(@AuthenticationPrincipal AuthenticatedUser caller,
                                             @PathVariable String tripId,
                                             @RequestBody Dtos.IngestPingsRequest request) {
        TripComputation out = service.ingestPings(caller.tenantId(), tripId, ApiMapper.toPings(request));
        return ApiMapper.toDistanceResponse(out.result(), out.appendedDelta());
    }

    @PostMapping("/{tripId}/recompute")
    public Dtos.DistanceResponse recompute(@AuthenticationPrincipal AuthenticatedUser caller,
                                           @PathVariable String tripId) {
        TripComputation out = service.recompute(caller.tenantId(), tripId, "manual-recompute");
        return ApiMapper.toDistanceResponse(out.result(), out.appendedDelta());
    }

    @PostMapping("/{tripId}/bill")
    public ResponseEntity<Void> bill(@AuthenticationPrincipal AuthenticatedUser caller,
                                     @PathVariable String tripId) {
        service.bill(caller.tenantId(), tripId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tripId}/deltas")
    public Dtos.DeltasResponse deltas(@AuthenticationPrincipal AuthenticatedUser caller,
                                      @PathVariable String tripId) {
        return ApiMapper.toDeltasResponse(tripId, service.ledger(caller.tenantId(), tripId));
    }
}
