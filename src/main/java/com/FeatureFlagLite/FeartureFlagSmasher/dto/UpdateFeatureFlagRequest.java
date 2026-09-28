package com.FeatureFlagLite.FeartureFlagSmasher.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing feature flag's metadata.
 */
public class UpdateFeatureFlagRequest {

    @Size(min = 2, max = 100, message = "Flag name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean defaultState;

    public UpdateFeatureFlagRequest() {
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

    public Boolean getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(Boolean defaultState) {
        this.defaultState = defaultState;
    }
}
