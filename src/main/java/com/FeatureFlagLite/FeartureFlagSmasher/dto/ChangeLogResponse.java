package com.FeatureFlagLite.FeartureFlagSmasher.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for a change log entry.
 */
public class ChangeLogResponse {

    private Long id;
    private String flagName;
    private String environment;
    private Boolean oldEnabled;
    private boolean newEnabled;
    private Integer oldRolloutPercentage;
    private int newRolloutPercentage;
    private String changedBy;
    private LocalDateTime changedAt;

    public ChangeLogResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getOldEnabled() {
        return oldEnabled;
    }

    public void setOldEnabled(Boolean oldEnabled) {
        this.oldEnabled = oldEnabled;
    }

    public boolean isNewEnabled() {
        return newEnabled;
    }

    public void setNewEnabled(boolean newEnabled) {
        this.newEnabled = newEnabled;
    }

    public Integer getOldRolloutPercentage() {
        return oldRolloutPercentage;
    }

    public void setOldRolloutPercentage(Integer oldRolloutPercentage) {
        this.oldRolloutPercentage = oldRolloutPercentage;
    }

    public int getNewRolloutPercentage() {
        return newRolloutPercentage;
    }

    public void setNewRolloutPercentage(int newRolloutPercentage) {
        this.newRolloutPercentage = newRolloutPercentage;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
