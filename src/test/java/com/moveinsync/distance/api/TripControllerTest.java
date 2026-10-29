package com.moveinsync.distance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.distance.api.dto.Dtos;
import com.moveinsync.distance.security.JwtService;
import com.moveinsync.distance.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JwtService jwt;

    private String bearer(String tenant, Role role) {
        return "Bearer " + jwt.issue(role.name().toLowerCase(), tenant, role);
    }

    private String computeBody(String tripId) throws Exception {
        Dtos.ComputeTripRequest req = new Dtos.ComputeTripRequest(
                tripId,
                List.of(new Dtos.PingDto(12.9000, 77.6000, T0, 10),
                        new Dtos.PingDto(12.9450, 77.6000, T0.plusSeconds(600), 10)),
                List.of(), List.of(), null, null, null, null, null);
        return json.writeValueAsString(req);
    }

    @Test
    void operatorCanComputeThenReadDistance() throws Exception {
        mvc.perform(post("/api/trips")
                        .header("Authorization", bearer("tenantA", Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("api-1"))
                .andExpect(jsonPath("$.status").value("OK"));

        mvc.perform(get("/api/trips/api-1/distance")
                        .header("Authorization", bearer("tenantA", Role.OPERATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("api-1"));
    }

    @Test
    void employeeCanReadButNotCompute() throws Exception {
        // Seed a trip as operator.
        mvc.perform(post("/api/trips")
                        .header("Authorization", bearer("tenantA", Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-emp")))
                .andExpect(status().isOk());

        // Employee may read it...
        mvc.perform(get("/api/trips/api-emp/explain")
                        .header("Authorization", bearer("tenantA", Role.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].rule").value("ROUTED"));

        // ...but not compute.
        mvc.perform(post("/api/trips")
                        .header("Authorization", bearer("tenantA", Role.EMPLOYEE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-emp-2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-2")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownTripIsNotFound() throws Exception {
        mvc.perform(get("/api/trips/does-not-exist/distance")
                        .header("Authorization", bearer("tenantA", Role.OPERATOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("trip_not_found"));
    }

    @Test
    void oneTenantCannotReadAnothersTrip() throws Exception {
        mvc.perform(post("/api/trips")
                        .header("Authorization", bearer("tenantA", Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-iso")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/trips/api-iso/distance")
                        .header("Authorization", bearer("tenantB", Role.OPERATOR)))
                .andExpect(status().isNotFound());
    }
}
