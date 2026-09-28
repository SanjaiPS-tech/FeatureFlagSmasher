package com.featureflaglite.featureflagsmasher.controller;

import com.featureflaglite.featureflagsmasher.service.FeatureFlagService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for environment management.
 */
@RestController
@RequestMapping("/api/v1/environments")
public class EnvironmentController {

    private final FeatureFlagService featureFlagService;

    public EnvironmentController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    /**
     * Get all available environments.
     */
    @GetMapping
    public ResponseEntity<List<String>> getAllEnvironments() {
        List<String> environments = featureFlagService.getAllEnvironments();
        return ResponseEntity.ok(environments);
    }
}
