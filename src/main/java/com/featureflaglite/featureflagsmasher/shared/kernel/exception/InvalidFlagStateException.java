package com.featureflaglite.featureflagsmasher.exception;

/**
 * Thrown when a flag state update contains invalid data.
 */
public class InvalidFlagStateException extends RuntimeException {

    public InvalidFlagStateException(String message) {
        super(message);
    }
}
