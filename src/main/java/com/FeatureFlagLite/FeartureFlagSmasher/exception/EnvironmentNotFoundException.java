package com.FeatureFlagLite.FeartureFlagSmasher.exception;

/**
 * Thrown when a requested environment is not found.
 */
public class EnvironmentNotFoundException extends RuntimeException {

    public EnvironmentNotFoundException(String message) {
        super(message);
    }
}
