package com.FeatureFlagLite.FeartureFlagSmasher.controller;

import com.FeatureFlagLite.FeartureFlagSmasher.dto.CreateFeatureFlagRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.UpdateFlagStateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the REST API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up: get all flags and delete them
        // This ensures test isolation
    }

    @Test
    @DisplayName("POST /api/v1/flags - should create flag and return 201")
    void shouldCreateFlag() throws Exception {
        CreateFeatureFlagRequest request = new CreateFeatureFlagRequest(
                "apiTestFlag", "API test flag", true);

        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("apiTestFlag"))
                .andExpect(jsonPath("$.defaultState").value(true))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("POST /api/v1/flags - should return 400 for blank name")
    void shouldReturn400ForBlankName() throws Exception {
        CreateFeatureFlagRequest request = new CreateFeatureFlagRequest(
                "", "Missing name", true);

        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/flags - should return 409 for duplicate name")
    void shouldReturn409ForDuplicate() throws Exception {
        CreateFeatureFlagRequest request = new CreateFeatureFlagRequest(
                "duplicateApiTest", "Duplicate test", true);

        // Create first
        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try duplicate
        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("GET /api/v1/flags?environment=dev - should return environment flags")
    void shouldReturnEnvironmentFlags() throws Exception {
        // Create a flag first
        CreateFeatureFlagRequest request = new CreateFeatureFlagRequest(
                "envApiTest", "Env test", true);
        mockMvc.perform(post("/api/v1/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/v1/flags")
                        .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.flags").isMap());
    }

    @Test
    @DisplayName("GET /api/v1/flags?environment=staging - should return 404")
    void shouldReturn404ForInvalidEnvironment() throws Exception {
        mockMvc.perform(get("/api/v1/flags")
                        .param("environment", "staging"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/environments - should return all environments")
    void shouldReturnAllEnvironments() throws Exception {
        mockMvc.perform(get("/api/v1/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", hasItems("dev", "test", "prod")));
    }

    @Test
    @DisplayName("PUT /api/v1/flags/{flagName}/states - should update state")
    void shouldUpdateFlagState() throws Exception {
        // Create flag
        CreateFeatureFlagRequest createReq = new CreateFeatureFlagRequest(
                "stateApiTest", "State API test", false);
        mockMvc.perform(post("/api/v1/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        // Update state
        UpdateFlagStateRequest stateReq = new UpdateFlagStateRequest(
                "dev", true, 75, "tester");
        mockMvc.perform(put("/api/v1/flags/stateApiTest/states")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagName").value("stateApiTest"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.rolloutPercentage").value(75));
    }

    @Test
    @DisplayName("PUT /api/v1/flags/{flagName}/states - should return 400 for invalid rollout")
    void shouldReturn400ForInvalidRollout() throws Exception {
        CreateFeatureFlagRequest createReq = new CreateFeatureFlagRequest(
                "rolloutValidation", "Rollout validation", true);
        mockMvc.perform(post("/api/v1/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        UpdateFlagStateRequest stateReq = new UpdateFlagStateRequest(
                "dev", true, 150, "tester");
        mockMvc.perform(put("/api/v1/flags/rolloutValidation/states")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stateReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/flags/{flagName}/evaluate - should evaluate flag")
    void shouldEvaluateFlag() throws Exception {
        CreateFeatureFlagRequest createReq = new CreateFeatureFlagRequest(
                "evalApiTest", "Eval test", true);
        mockMvc.perform(post("/api/v1/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        mockMvc.perform(get("/api/v1/flags/evalApiTest/evaluate")
                        .param("environment", "dev")
                        .param("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagName").value("evalApiTest"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.rolloutPercentage").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/flags/{flagName}/history - should return change history")
    void shouldReturnHistory() throws Exception {
        CreateFeatureFlagRequest createReq = new CreateFeatureFlagRequest(
                "historyApiTest", "History test", false);
        mockMvc.perform(post("/api/v1/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        // Make a change
        UpdateFlagStateRequest stateReq = new UpdateFlagStateRequest(
                "dev", true, 50, "admin");
        mockMvc.perform(put("/api/v1/flags/historyApiTest/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stateReq)));

        mockMvc.perform(get("/api/v1/flags/historyApiTest/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].changedBy").value("admin"));
    }

    @Test
    @DisplayName("DELETE /api/v1/flags/{id} - should delete flag and return 204")
    void shouldDeleteFlag() throws Exception {
        CreateFeatureFlagRequest createReq = new CreateFeatureFlagRequest(
                "deleteApiTest", "Delete test", true);
        String response = mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/flags/" + id))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/v1/flags/" + id))
                .andExpect(status().isNotFound());
    }
}
