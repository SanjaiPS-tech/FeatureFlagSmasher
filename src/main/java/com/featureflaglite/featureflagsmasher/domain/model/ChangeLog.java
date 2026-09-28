package com.featureflaglite.featureflagsmasher.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an audit log entry for flag state changes.
 * Records who changed what, when, and the before/after values.
 */
public class ChangeLog {

    private Long id;
    private FeatureFlag featureFlag;
    private Environment environment;
    private Boolean oldEnabled;
    private boolean newEnabled;
    private Integer oldRolloutPercentage;
    private int newRolloutPercentage;
    private String changedBy;
    private LocalDateTime changedAt;

    protected ChangeLog() {
        // For JPA
    }

    public ChangeLog(FeatureFlag featureFlag, Environment environment,
                     Boolean oldEnabled, boolean newEnabled,
                     Integer oldRolloutPercentage, int newRolloutPercentage,
                     String changedBy) {
        this.featureFlag = Objects.requireNonNull(featureFlag, "FeatureFlag cannot be null");
        this.environment = Objects.requireNonNull(environment, "Environment cannot be null");
        this.oldEnabled = oldEnabled;
        this.newEnabled = newEnabled;
        this.oldRolloutPercentage = oldRolloutPercentage;
        this.newRolloutPercentage = validateRolloutPercentage(newRolloutPercentage);
        this.changedBy = Objects.requireNonNull(changedBy, "ChangedBy cannot be null");
        this.changedAt = LocalDateTime.now();
    }

    private int validateRolloutPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
        }
        return percentage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeatureFlag getFeatureFlag() {
        return featureFlag;
    }

    public void setFeatureFlag(FeatureFlag featureFlag) {
        this.featureFlag = Objects.requireNonNull(featureFlag, "FeatureFlag cannot be null");
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "Environment cannot be null");
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
        this.newRolloutPercentage = validateRolloutPercentage(newRolloutPercentage);
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = Objects.requireNonNull(changedBy, "ChangedBy cannot be null");
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeLog that = (ChangeLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ChangeLog{id=" + id + ", flag=" + featureFlag.getName() +
                ", env=" + environment.getName() + ", newEnabled=" + newEnabled +
                ", rollout=" + newRolloutPercentage + "%, by=" + changedBy + "}";
    }
}