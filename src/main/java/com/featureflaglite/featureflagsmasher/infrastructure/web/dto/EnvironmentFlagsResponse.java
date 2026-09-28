package com.featureflaglite.featureflagsmasher.dto;

import java.util.Map;

/**
 * Response DTO for retrieving all flags in a specific environment.
 */
public class EnvironmentFlagsResponse {

    private String environment;
    private Map<String, Boolean> flags;

    public EnvironmentFlagsResponse() {
    }

    public EnvironmentFlagsResponse(String environment, Map<String, Boolean> flags) {
        this.environment = environment;
        this.flags = flags;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public void setFlags(Map<String, Boolean> flags) {
        this.flags = flags;
    }
}
