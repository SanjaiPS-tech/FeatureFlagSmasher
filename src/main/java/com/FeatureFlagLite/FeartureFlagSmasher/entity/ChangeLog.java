package com.FeatureFlagLite.FeartureFlagSmasher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Stores the audit trail of flag state changes.
 * Records who changed what, when, and the before/after values.
 */
@Entity
@Table(name = "change_logs")
public class ChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(name = "old_enabled")
    private Boolean oldEnabled;

    @Column(name = "new_enabled", nullable = false)
    private boolean newEnabled;

    @Column(name = "old_rollout_percentage")
    private Integer oldRolloutPercentage;

    @Column(name = "new_rollout_percentage", nullable = false)
    private int newRolloutPercentage;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public ChangeLog() {
    }

    public ChangeLog(FeatureFlag featureFlag, Environment environment, Boolean oldEnabled, boolean newEnabled,
                     Integer oldRolloutPercentage, int newRolloutPercentage, String changedBy) {
        this.featureFlag = featureFlag;
        this.environment = environment;
        this.oldEnabled = oldEnabled;
        this.newEnabled = newEnabled;
        this.oldRolloutPercentage = oldRolloutPercentage;
        this.newRolloutPercentage = newRolloutPercentage;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
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
        this.featureFlag = featureFlag;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
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
