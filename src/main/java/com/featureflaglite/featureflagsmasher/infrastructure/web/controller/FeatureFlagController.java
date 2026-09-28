package com.featureflaglite.featureflagsmasher.controller;

import com.featureflaglite.featureflagsmasher.dto.ChangeLogResponse;
import com.featureflaglite.featureflagsmasher.dto.CreateFeatureFlagRequest;
import com.featureflaglite.featureflagsmasher.dto.EnvironmentFlagsResponse;
import com.featureflaglite.featureflagsmasher.dto.FeatureFlagResponse;
import com.featureflaglite.featureflagsmasher.dto.FlagEvaluationResponse;
import com.featureflaglite.featureflagsmasher.dto.FlagStateResponse;
import com.featureflaglite.featureflagsmasher.dto.UpdateFeatureFlagRequest;
import com.featureflaglite.featureflagsmasher.dto.UpdateFlagStateRequest;
import com.featureflaglite.featureflagsmasher.service.FeatureFlagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for feature flag management.
 * Handles all CRUD operations, state management, evaluation, and history retrieval.
 */
@RestController
@RequestMapping("/api/v1/flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final com.featureflaglite.featureflagsmasher.service.FlagEventPublisher flagEventPublisher;

    public FeatureFlagController(FeatureFlagService featureFlagService,
                                 com.featureflaglite.featureflagsmasher.service.FlagEventPublisher flagEventPublisher) {
        this.featureFlagService = featureFlagService;
        this.flagEventPublisher = flagEventPublisher;
    }

    // ──────────────────────────────────────────────
    // Feature Flag CRUD
    // ──────────────────────────────────────────────

    /**
     * Create a new feature flag.
     */
    @PostMapping
    public ResponseEntity<FeatureFlagResponse> createFeatureFlag(
            @Valid @RequestBody CreateFeatureFlagRequest request) {
        FeatureFlagResponse response = featureFlagService.createFeatureFlag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all feature flags.
     */
    @GetMapping
    public ResponseEntity<?> getFlags(
            @RequestParam(required = false) String environment) {
        if (environment != null && !environment.isBlank()) {
            EnvironmentFlagsResponse response = featureFlagService.getFlagsForEnvironment(environment);
            return ResponseEntity.ok(response);
        }
        List<FeatureFlagResponse> response = featureFlagService.getAllFeatureFlags();
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // Administrative & System Endpoints (Ordered before /{id} & /{flagName})
    // ──────────────────────────────────────────────

    /**
     * Real-time Server-Sent Events (SSE) stream.
     * Pushes instant notifications whenever flags are created, updated, or toggled.
     */
    @GetMapping(value = {"/stream", "/system/stream"}, produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamFlagEvents() {
        return flagEventPublisher.registerEmitter();
    }

    /**
     * Get the global system audit log across all flags and environments.
     */
    @GetMapping({"/audit", "/system/audit"})
    public ResponseEntity<List<ChangeLogResponse>> getGlobalAuditLog() {
        List<ChangeLogResponse> response = featureFlagService.getAllChangeHistory();
        return ResponseEntity.ok(response);
    }

    /**
     * Manually purge all in-memory caches.
     */
    @PostMapping({"/cache/purge", "/system/cache/purge"})
    public ResponseEntity<java.util.Map<String, Object>> purgeCache() {
        featureFlagService.purgeAllCaches();
        return ResponseEntity.ok(java.util.Map.of(
                "status", "SUCCESS",
                "message", "All in-memory caches have been successfully purged. Downstream evaluations will read fresh state directly from the database."
        ));
    }

    /**
     * Evaluates all registered feature flags for a given user and environment.
     */
    @GetMapping("/system/evaluate-all")
    public ResponseEntity<List<FlagEvaluationResponse>> evaluateAllForUser(
            @RequestParam String environment,
            @RequestParam(required = false) String userId) {
        List<FlagEvaluationResponse> response = featureFlagService.evaluateAllFlagsForUser(environment, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Syncs / promotes all flag states from a source environment to a target environment.
     */
    @PostMapping("/system/sync")
    public ResponseEntity<java.util.Map<String, Object>> syncEnvironments(
            @RequestParam String sourceEnv,
            @RequestParam String targetEnv,
            @RequestParam(required = false, defaultValue = "admin") String changedBy) {
        int count = featureFlagService.syncEnvironmentStates(sourceEnv, targetEnv, changedBy);
        return ResponseEntity.ok(java.util.Map.of(
                "status", "SUCCESS",
                "message", "Successfully synced " + count + " flag configuration(s) from " + sourceEnv.toUpperCase() + " to " + targetEnv.toUpperCase() + ".",
                "syncedCount", count
        ));
    }

    /**
     * Emergency Killswitch: Pauses all active flags in the specified environment.
     */
    @PostMapping("/system/killswitch")
    public ResponseEntity<java.util.Map<String, Object>> emergencyKillswitch(
            @RequestParam String environment,
            @RequestParam(required = false, defaultValue = "admin-killswitch") String changedBy) {
        int count = featureFlagService.emergencyKillswitch(environment, changedBy);
        return ResponseEntity.ok(java.util.Map.of(
                "status", "SUCCESS",
                "message", "Emergency Killswitch executed: " + count + " flag(s) safely paused in " + environment.toUpperCase() + " to protect system stability.",
                "pausedCount", count
        ));
    }

    /**
     * Get all available environments.
     */
    @GetMapping("/system/environments")
    public ResponseEntity<List<String>> getEnvironments() {
        return ResponseEntity.ok(featureFlagService.getAllEnvironments());
    }

    /**
     * Get a feature flag by ID.
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<FeatureFlagResponse> getFeatureFlagById(@PathVariable Long id) {
        FeatureFlagResponse response = featureFlagService.getFeatureFlagById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a feature flag by name.
     */
    @GetMapping("/{flagName:[a-zA-Z]\\w*}")
    public ResponseEntity<FeatureFlagResponse> getFeatureFlagByName(@PathVariable String flagName) {
        FeatureFlagResponse response = featureFlagService.getFeatureFlagByName(flagName);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a feature flag's metadata.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FeatureFlagResponse> updateFeatureFlag(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeatureFlagRequest request) {
        FeatureFlagResponse response = featureFlagService.updateFeatureFlag(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a feature flag.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeatureFlag(@PathVariable Long id) {
        featureFlagService.deleteFeatureFlag(id);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────
    // Flag States
    // ──────────────────────────────────────────────

    /**
     * Get all states for a flag across environments.
     */
    @GetMapping("/{flagName}/states")
    public ResponseEntity<List<FlagStateResponse>> getFlagStates(@PathVariable String flagName) {
        List<FlagStateResponse> response = featureFlagService.getFlagStates(flagName);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a flag's state in a specific environment.
     */
    @PutMapping("/{flagName}/states")
    public ResponseEntity<FlagStateResponse> updateFlagState(
            @PathVariable String flagName,
            @Valid @RequestBody UpdateFlagStateRequest request) {
        FlagStateResponse response = featureFlagService.updateFlagState(flagName, request);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // Evaluation
    // ──────────────────────────────────────────────

    /**
     * Evaluate a flag for a specific user using percentage rollout.
     */
    @GetMapping("/{flagName}/evaluate")
    public ResponseEntity<FlagEvaluationResponse> evaluateFlag(
            @PathVariable String flagName,
            @RequestParam String environment,
            @RequestParam(required = false) String userId) {
        FlagEvaluationResponse response = featureFlagService.evaluateFlag(flagName, environment, userId);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // Change History
    // ──────────────────────────────────────────────

    /**
     * Get the change history for a flag.
     */
    @GetMapping("/{flagName}/history")
    public ResponseEntity<List<ChangeLogResponse>> getChangeHistory(@PathVariable String flagName) {
        List<ChangeLogResponse> response = featureFlagService.getChangeHistory(flagName);
        return ResponseEntity.ok(response);
    }
}
