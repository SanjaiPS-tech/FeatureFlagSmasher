package com.FeatureFlagLite.FeartureFlagSmasher.exception;

/**
 * Thrown when a requested feature flag is not found.
 */
public class FeatureFlagNotFoundException extends RuntimeException {

    public FeatureFlagNotFoundException(String message) {
        super(message);
    }
}
