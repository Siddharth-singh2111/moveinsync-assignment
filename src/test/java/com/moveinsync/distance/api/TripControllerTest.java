package com.moveinsync.distance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.distance.api.dto.Dtos;
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

    private String computeBody(String tripId) throws Exception {
        Dtos.ComputeTripRequest req = new Dtos.ComputeTripRequest(
                tripId,
                List.of(new Dtos.PingDto(12.9000, 77.6000, T0, 10),
                        new Dtos.PingDto(12.9450, 77.6000, T0.plusSeconds(600), 10)),
                List.of(), List.of(), null, null, null, null, null);
        return json.writeValueAsString(req);
    }

    @Test
    void computeThenReadDistance() throws Exception {
        mvc.perform(post("/api/trips")
                        .header("X-Tenant-Id", "tenantA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("api-1"))
                .andExpect(jsonPath("$.totalMeters").isNumber())
                .andExpect(jsonPath("$.status").value("OK"));

        mvc.perform(get("/api/trips/api-1/distance").header("X-Tenant-Id", "tenantA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value("api-1"));
    }

    @Test
    void explainReturnsLabelledSegments() throws Exception {
        mvc.perform(post("/api/trips")
                        .header("X-Tenant-Id", "tenantA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-explain")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/trips/api-explain/explain").header("X-Tenant-Id", "tenantA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].rule").value("ROUTED"));
    }

    @Test
    void missingTenantHeaderIsBadRequest() throws Exception {
        mvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(computeBody("api-2")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTripIsNotFound() throws Exception {
        mvc.perform(get("/api/trips/does-not-exist/distance").header("X-Tenant-Id", "tenantA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("trip_not_found"));
    }
}
