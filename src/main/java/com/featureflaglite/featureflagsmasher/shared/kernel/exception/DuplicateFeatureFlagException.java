package com.featureflaglite.featureflagsmasher.exception;

/**
 * Thrown when attempting to create a feature flag with a name that already exists.
 */
public class DuplicateFeatureFlagException extends RuntimeException {

    public DuplicateFeatureFlagException(String message) {
        super(message);
    }
}
