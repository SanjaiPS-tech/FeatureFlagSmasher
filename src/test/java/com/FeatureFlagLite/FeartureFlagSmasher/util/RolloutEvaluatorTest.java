package com.FeatureFlagLite.FeartureFlagSmasher.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the deterministic rollout evaluator.
 */
class RolloutEvaluatorTest {

    @Test
    @DisplayName("Should return true for 100% rollout")
    void shouldReturnTrueForFullRollout() {
        assertTrue(RolloutEvaluator.isUserInRollout("flag", "user1", 100));
        assertTrue(RolloutEvaluator.isUserInRollout("flag", "user2", 100));
        assertTrue(RolloutEvaluator.isUserInRollout("flag", "user999", 100));
    }

    @Test
    @DisplayName("Should return false for 0% rollout")
    void shouldReturnFalseForZeroRollout() {
        assertFalse(RolloutEvaluator.isUserInRollout("flag", "user1", 0));
        assertFalse(RolloutEvaluator.isUserInRollout("flag", "user2", 0));
        assertFalse(RolloutEvaluator.isUserInRollout("flag", "user999", 0));
    }

    @Test
    @DisplayName("Should be deterministic for same inputs")
    void shouldBeDeterministic() {
        boolean first = RolloutEvaluator.isUserInRollout("testFlag", "user-42", 50);
        boolean second = RolloutEvaluator.isUserInRollout("testFlag", "user-42", 50);
        boolean third = RolloutEvaluator.isUserInRollout("testFlag", "user-42", 50);

        assertEquals(first, second);
        assertEquals(second, third);
    }

    @Test
    @DisplayName("Should produce roughly expected distribution at 50%")
    void shouldProduceExpectedDistribution() {
        int enabled = 0;
        int total = 10000;

        for (int i = 0; i < total; i++) {
            if (RolloutEvaluator.isUserInRollout("distributionTest", "user-" + i, 50)) {
                enabled++;
            }
        }

        double percentage = (double) enabled / total * 100;
        // With 10k samples, expect 45-55% range
        assertTrue(percentage > 40 && percentage < 60,
                "Expected ~50% but got " + percentage + "%");
    }

    @Test
    @DisplayName("Bucket should be in range 0-99")
    void bucketShouldBeInRange() {
        for (int i = 0; i < 1000; i++) {
            int bucket = RolloutEvaluator.computeBucket("flag:user-" + i);
            assertTrue(bucket >= 0 && bucket < 100,
                    "Bucket " + bucket + " out of range for user-" + i);
        }
    }

    @Test
    @DisplayName("Different flags should produce different results for same user")
    void differentFlagsShouldVary() {
        // Not all flags should give the same result for the same user
        int sameResult = 0;
        boolean baseline = RolloutEvaluator.isUserInRollout("flag-baseline", "user-test", 50);

        for (int i = 0; i < 100; i++) {
            boolean result = RolloutEvaluator.isUserInRollout("flag-" + i, "user-test", 50);
            if (result == baseline) sameResult++;
        }

        // At least some should differ
        assertTrue(sameResult < 90, "Too many flags gave the same result: " + sameResult);
    }
}
