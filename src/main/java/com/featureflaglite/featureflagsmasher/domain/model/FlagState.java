package com.featureflaglite.featureflagsmasher.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing the state of a feature flag within a specific environment.
 * Each flag has exactly one state per environment.
 */
public class FlagState {

    private Long id;
    private FeatureFlag featureFlag;
    private Environment environment;
    private boolean enabled;
    private int rolloutPercentage;
    private LocalDateTime updatedAt;

    protected FlagState() {
        // For JPA
    }

    public FlagState(FeatureFlag featureFlag, Environment environment, boolean enabled, int rolloutPercentage) {
        this.featureFlag = Objects.requireNonNull(featureFlag, "FeatureFlag cannot be null");
        this.environment = Objects.requireNonNull(environment, "Environment cannot be null");
        this.enabled = enabled;
        this.rolloutPercentage = validateRolloutPercentage(rolloutPercentage);
        this.updatedAt = LocalDateTime.now();
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
        this.updatedAt = LocalDateTime.now();
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "Environment cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(int rolloutPercentage) {
        this.rolloutPercentage = validateRolloutPercentage(rolloutPercentage);
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void updateState(boolean enabled, int rolloutPercentage) {
        this.enabled = enabled;
        this.rolloutPercentage = validateRolloutPercentage(rolloutPercentage);
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlagState that = (FlagState) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(featureFlag, that.featureFlag) &&
                Objects.equals(environment, that.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, featureFlag, environment);
    }

    @Override
    public String toString() {
        return "FlagState{id=" + id + ", flag=" + featureFlag.getName() +
                ", env=" + environment.getName() + ", enabled=" + enabled +
                ", rollout=" + rolloutPercentage + "%}";
    }
}