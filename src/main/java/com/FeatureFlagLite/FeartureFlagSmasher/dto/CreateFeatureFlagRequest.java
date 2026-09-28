package com.FeatureFlagLite.FeartureFlagSmasher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new feature flag.
 */
public class CreateFeatureFlagRequest {

    @NotBlank(message = "Flag name is required")
    @Size(min = 2, max = 100, message = "Flag name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private boolean defaultState;

    public CreateFeatureFlagRequest() {
    }

    public CreateFeatureFlagRequest(String name, String description, boolean defaultState) {
        this.name = name;
        this.description = description;
        this.defaultState = defaultState;
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
}
