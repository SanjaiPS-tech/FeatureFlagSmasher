package com.featureflaglite.featureflagsmasher.dto;

/**
 * Response DTO for evaluating a flag for a specific user.
 */
public class FlagEvaluationResponse {

    private String flagName;
    private String environment;
    private boolean enabled;
    private int rolloutPercentage;
    private Integer bucket;
    private String explanation;

    public FlagEvaluationResponse() {
    }

    public FlagEvaluationResponse(String flagName, String environment, boolean enabled, int rolloutPercentage) {
        this.flagName = flagName;
        this.environment = environment;
        this.enabled = enabled;
        this.rolloutPercentage = rolloutPercentage;
    }

    public FlagEvaluationResponse(String flagName, String environment, boolean enabled, int rolloutPercentage, Integer bucket, String explanation) {
        this.flagName = flagName;
        this.environment = environment;
        this.enabled = enabled;
        this.rolloutPercentage = rolloutPercentage;
        this.bucket = bucket;
        this.explanation = explanation;
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

    public Integer getBucket() {
        return bucket;
    }

    public void setBucket(Integer bucket) {
        this.bucket = bucket;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
