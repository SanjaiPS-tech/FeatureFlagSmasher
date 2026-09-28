package com.featureflaglite.featureflagsmasher.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for a flag's state within an environment.
 */
public class FlagStateResponse {

    private String flagName;
    private String environment;
    private boolean enabled;
    private int rolloutPercentage;
    private LocalDateTime updatedAt;

    public FlagStateResponse() {
    }

    public FlagStateResponse(String flagName, String environment, boolean enabled,
                              int rolloutPercentage, LocalDateTime updatedAt) {
        this.flagName = flagName;
        this.environment = environment;
        this.enabled = enabled;
        this.rolloutPercentage = rolloutPercentage;
        this.updatedAt = updatedAt;
    }

    public String getFlagName() {
        return flagName;
    }

    public void setFlagName(String flagName) {
        this.flagName = flagName;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
