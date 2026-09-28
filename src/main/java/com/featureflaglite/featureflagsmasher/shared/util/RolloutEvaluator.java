package com.featureflaglite.featureflagsmasher.util;

/**
 * Utility for deterministic percentage rollout evaluation.
 * Uses a hash-based approach so the same user always gets the same result for the same flag.
 */
public final class RolloutEvaluator {

    private static final int PERCENTAGE_MAX = 100;

    private RolloutEvaluator() {
        // Prevent instantiation
    }

    /**
     * Determines whether a user should see a feature based on rollout percentage.
     * Uses a deterministic hash of flagName + userId to produce a consistent bucket (0-99).
     *
     * @param flagName           the name of the feature flag
     * @param userId             the user identifier
     * @param rolloutPercentage  the rollout percentage (0-100)
     * @return true if the user falls within the rollout percentage
     */
    public static boolean isUserInRollout(String flagName, String userId, int rolloutPercentage) {
        if (rolloutPercentage >= PERCENTAGE_MAX) {
            return true;
        }
        if (rolloutPercentage <= 0) {
            return false;
        }

        String key = flagName + ":" + userId;
        int bucket = computeBucket(key);
        return bucket < rolloutPercentage;
    }

    /**
     * Gets the bucket index (0-99) for a given flag and user ID.
     */
    public static int getBucket(String flagName, String userId) {
        if (userId == null || userId.isBlank()) {
            return 0;
        }
        return computeBucket(flagName + ":" + userId);
    }

    /**
     * Computes a deterministic bucket (0-99) from a key string.
     * Uses a simple but effective hash function that distributes evenly.
     */
    static int computeBucket(String key) {
        // Use Java's String.hashCode() with Math.abs and modulo for deterministic bucketing
        int hash = key.hashCode();
        // Handle Integer.MIN_VALUE edge case
        int positiveHash = (hash == Integer.MIN_VALUE) ? 0 : Math.abs(hash);
        return positiveHash % PERCENTAGE_MAX;
    }
}
