package com.featureflaglite.featureflagsmasher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating a flag's state within a specific environment.
 */
public class UpdateFlagStateRequest {

    @NotBlank(message = "Environment name is required")
    private String environment;

    private boolean enabled;

    @Min(value = 0, message = "rolloutPercentage must be between 0 and 100")
    @Max(value = 100, message = "rolloutPercentage must be between 0 and 100")
    private int rolloutPercentage;

    @NotBlank(message = "changedBy is required")
    private String changedBy;

    public UpdateFlagStateRequest() {
    }

    public UpdateFlagStateRequest(String environment, boolean enabled, int rolloutPercentage, String changedBy) {
        this.environment = environment;
        this.enabled = enabled;
        this.rolloutPercentage = rolloutPercentage;
        this.changedBy = changedBy;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(int rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
