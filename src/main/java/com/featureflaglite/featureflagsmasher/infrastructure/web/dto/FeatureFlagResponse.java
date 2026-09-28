package com.featureflaglite.featureflagsmasher.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for a feature flag.
 */
public class FeatureFlagResponse {

    private Long id;
    private String name;
    private String description;
    private boolean defaultState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FeatureFlagResponse() {
    }

    public FeatureFlagResponse(Long id, String name, String description, boolean defaultState,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultState = defaultState;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultState() {
        return defaultState;
    }

    public void setDefaultState(boolean defaultState) {
        this.defaultState = defaultState;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
