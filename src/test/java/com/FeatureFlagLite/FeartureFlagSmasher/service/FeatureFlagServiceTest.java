package com.FeatureFlagLite.FeartureFlagSmasher.service;

import com.FeatureFlagLite.FeartureFlagSmasher.dto.CreateFeatureFlagRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.EnvironmentFlagsResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.FeatureFlagResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.FlagEvaluationResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.FlagStateResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.UpdateFlagStateRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.ChangeLogResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.UpdateFeatureFlagRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.exception.DuplicateFeatureFlagException;
import com.FeatureFlagLite.FeartureFlagSmasher.exception.EnvironmentNotFoundException;
import com.FeatureFlagLite.FeartureFlagSmasher.exception.FeatureFlagNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for FeatureFlagService.
 * Uses H2 in-memory database for isolated testing.
 */
@SpringBootTest
@ActiveProfiles("test")
class FeatureFlagServiceTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    private FeatureFlagResponse createdFlag;

    @BeforeEach
    void setUp() {
        // Clean up existing flags
        List<FeatureFlagResponse> existingFlags = featureFlagService.getAllFeatureFlags();
        for (FeatureFlagResponse flag : existingFlags) {
            featureFlagService.deleteFeatureFlag(flag.getId());
        }
    }

    private FeatureFlagResponse createTestFlag(String name, String description, boolean defaultState) {
        CreateFeatureFlagRequest request = new CreateFeatureFlagRequest(name, description, defaultState);
        return featureFlagService.createFeatureFlag(request);
    }

    // ──────────────────────────────────────────────
    // Feature Flag CRUD Tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Create Feature Flag")
    class CreateFeatureFlagTests {

        @Test
        @DisplayName("Should create feature flag successfully")
        void shouldCreateFeatureFlag() {
            FeatureFlagResponse response = createTestFlag("newDashboard", "New dashboard feature", true);

            assertNotNull(response.getId());
            assertEquals("newDashboard", response.getName());
            assertEquals("New dashboard feature", response.getDescription());
            assertTrue(response.isDefaultState());
            assertNotNull(response.getCreatedAt());
            assertNotNull(response.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create flag with default state OFF")
        void shouldCreateFlagWithDefaultOff() {
            FeatureFlagResponse response = createTestFlag("darkMode", "Dark mode toggle", false);

            assertFalse(response.isDefaultState());
        }

        @Test
        @DisplayName("Should throw DuplicateFeatureFlagException for duplicate name")
        void shouldThrowDuplicateException() {
            createTestFlag("testFlag", "First flag", true);

            assertThrows(DuplicateFeatureFlagException.class, () ->
                    createTestFlag("testFlag", "Duplicate flag", false));
        }

        @Test
        @DisplayName("Should initialize flag states for all environments")
        void shouldInitializeFlagStatesForAllEnvironments() {
            FeatureFlagResponse flag = createTestFlag("envFlag", "Environment test", true);

            List<FlagStateResponse> states = featureFlagService.getFlagStates("envFlag");

            assertEquals(3, states.size());
            assertTrue(states.stream().anyMatch(s -> s.getEnvironment().equals("dev")));
            assertTrue(states.stream().anyMatch(s -> s.getEnvironment().equals("test")));
            assertTrue(states.stream().anyMatch(s -> s.getEnvironment().equals("prod")));
        }
    }

    @Nested
    @DisplayName("Get Feature Flag")
    class GetFeatureFlagTests {

        @Test
        @DisplayName("Should get flag by ID")
        void shouldGetFlagById() {
            FeatureFlagResponse created = createTestFlag("getById", "Test get by ID", true);

            FeatureFlagResponse result = featureFlagService.getFeatureFlagById(created.getId());

            assertEquals(created.getId(), result.getId());
            assertEquals("getById", result.getName());
        }

        @Test
        @DisplayName("Should get flag by name")
        void shouldGetFlagByName() {
            createTestFlag("getByName", "Test get by name", false);

            FeatureFlagResponse result = featureFlagService.getFeatureFlagByName("getByName");

            assertEquals("getByName", result.getName());
        }

        @Test
        @DisplayName("Should throw FeatureFlagNotFoundException for unknown ID")
        void shouldThrowNotFoundForUnknownId() {
            assertThrows(FeatureFlagNotFoundException.class, () ->
                    featureFlagService.getFeatureFlagById(99999L));
        }

        @Test
        @DisplayName("Should throw FeatureFlagNotFoundException for unknown name")
        void shouldThrowNotFoundForUnknownName() {
            assertThrows(FeatureFlagNotFoundException.class, () ->
                    featureFlagService.getFeatureFlagByName("nonExistent"));
        }

        @Test
        @DisplayName("Should get all flags")
        void shouldGetAllFlags() {
            createTestFlag("flag1", "First", true);
            createTestFlag("flag2", "Second", false);

            List<FeatureFlagResponse> all = featureFlagService.getAllFeatureFlags();

            assertEquals(2, all.size());
        }
    }

    @Nested
    @DisplayName("Update Feature Flag")
    class UpdateFeatureFlagTests {

        @Test
        @DisplayName("Should update flag name and description")
        void shouldUpdateFlag() {
            FeatureFlagResponse created = createTestFlag("oldName", "Old desc", true);

            UpdateFeatureFlagRequest update = new UpdateFeatureFlagRequest();
            update.setName("newName");
            update.setDescription("New desc");

            FeatureFlagResponse updated = featureFlagService.updateFeatureFlag(created.getId(), update);

            assertEquals("newName", updated.getName());
            assertEquals("New desc", updated.getDescription());
        }

        @Test
        @DisplayName("Should throw DuplicateFeatureFlagException when renaming to existing name")
        void shouldThrowDuplicateOnRename() {
            createTestFlag("existingName", "Existing", true);
            FeatureFlagResponse toRename = createTestFlag("originalName", "To rename", false);

            UpdateFeatureFlagRequest update = new UpdateFeatureFlagRequest();
            update.setName("existingName");

            assertThrows(DuplicateFeatureFlagException.class, () ->
                    featureFlagService.updateFeatureFlag(toRename.getId(), update));
        }
    }

    @Nested
    @DisplayName("Delete Feature Flag")
    class DeleteFeatureFlagTests {

        @Test
        @DisplayName("Should delete flag and associated states")
        void shouldDeleteFlag() {
            FeatureFlagResponse created = createTestFlag("toDelete", "Will be deleted", true);

            featureFlagService.deleteFeatureFlag(created.getId());

            assertThrows(FeatureFlagNotFoundException.class, () ->
                    featureFlagService.getFeatureFlagById(created.getId()));
        }

        @Test
        @DisplayName("Should throw FeatureFlagNotFoundException when deleting unknown flag")
        void shouldThrowNotFoundOnDelete() {
            assertThrows(FeatureFlagNotFoundException.class, () ->
                    featureFlagService.deleteFeatureFlag(99999L));
        }
    }

    // ──────────────────────────────────────────────
    // Flag State Tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Environment-Specific State")
    class EnvironmentStateTests {

        @Test
        @DisplayName("Should update flag state for specific environment")
        void shouldUpdateFlagState() {
            createTestFlag("stateTest", "State test flag", false);

            UpdateFlagStateRequest request = new UpdateFlagStateRequest("dev", true, 100, "admin");
            FlagStateResponse response = featureFlagService.updateFlagState("stateTest", request);

            assertEquals("stateTest", response.getFlagName());
            assertEquals("dev", response.getEnvironment());
            assertTrue(response.isEnabled());
            assertEquals(100, response.getRolloutPercentage());
        }

        @Test
        @DisplayName("Should change one environment without affecting others")
        void shouldNotAffectOtherEnvironments() {
            createTestFlag("isolatedState", "Isolation test", false);

            // Enable in dev only
            featureFlagService.updateFlagState("isolatedState",
                    new UpdateFlagStateRequest("dev", true, 100, "admin"));

            // Verify test and prod are still off
            EnvironmentFlagsResponse devFlags = featureFlagService.getFlagsForEnvironment("dev");
            EnvironmentFlagsResponse testFlags = featureFlagService.getFlagsForEnvironment("test");
            EnvironmentFlagsResponse prodFlags = featureFlagService.getFlagsForEnvironment("prod");

            assertTrue(devFlags.getFlags().get("isolatedState"));
            assertFalse(testFlags.getFlags().get("isolatedState"));
            assertFalse(prodFlags.getFlags().get("isolatedState"));
        }

        @Test
        @DisplayName("Should throw EnvironmentNotFoundException for invalid environment")
        void shouldThrowForInvalidEnvironment() {
            createTestFlag("envTest", "Env test", true);

            assertThrows(EnvironmentNotFoundException.class, () ->
                    featureFlagService.updateFlagState("envTest",
                            new UpdateFlagStateRequest("staging", true, 100, "admin")));
        }
    }

    // ──────────────────────────────────────────────
    // Percentage Rollout Tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Percentage Rollout")
    class PercentageRolloutTests {

        @Test
        @DisplayName("Should disable for 0% rollout")
        void shouldDisableForZeroRollout() {
            createTestFlag("zeroRollout", "Zero rollout", true);
            featureFlagService.updateFlagState("zeroRollout",
                    new UpdateFlagStateRequest("dev", true, 0, "admin"));

            FlagEvaluationResponse result = featureFlagService.evaluateFlag("zeroRollout", "dev", "user123");

            assertFalse(result.isEnabled());
            assertEquals(0, result.getRolloutPercentage());
        }

        @Test
        @DisplayName("Should enable for 100% rollout")
        void shouldEnableForFullRollout() {
            createTestFlag("fullRollout", "Full rollout", true);
            featureFlagService.updateFlagState("fullRollout",
                    new UpdateFlagStateRequest("dev", true, 100, "admin"));

            FlagEvaluationResponse result = featureFlagService.evaluateFlag("fullRollout", "dev", "user123");

            assertTrue(result.isEnabled());
            assertEquals(100, result.getRolloutPercentage());
        }

        @Test
        @DisplayName("Should be deterministic - same user gets same result")
        void shouldBeDeterministic() {
            createTestFlag("deterministicFlag", "Deterministic test", true);
            featureFlagService.updateFlagState("deterministicFlag",
                    new UpdateFlagStateRequest("dev", true, 50, "admin"));

            FlagEvaluationResponse first = featureFlagService.evaluateFlag("deterministicFlag", "dev", "user-42");
            FlagEvaluationResponse second = featureFlagService.evaluateFlag("deterministicFlag", "dev", "user-42");

            assertEquals(first.isEnabled(), second.isEnabled());
        }

        @Test
        @DisplayName("Should produce varied results across different users")
        void shouldProduceVariedResults() {
            createTestFlag("variedRollout", "Varied rollout test", true);
            featureFlagService.updateFlagState("variedRollout",
                    new UpdateFlagStateRequest("dev", true, 50, "admin"));

            int enabledCount = 0;
            int totalUsers = 1000;

            for (int i = 0; i < totalUsers; i++) {
                FlagEvaluationResponse result = featureFlagService.evaluateFlag(
                        "variedRollout", "dev", "user-" + i);
                if (result.isEnabled()) enabledCount++;
            }

            // With 50% rollout and 1000 users, expect roughly 40-60% enabled
            double percentage = (double) enabledCount / totalUsers * 100;
            assertTrue(percentage > 30 && percentage < 70,
                    "Expected ~50% but got " + percentage + "%");
        }
    }

    // ──────────────────────────────────────────────
    // Read API Tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Read API")
    class ReadApiTests {

        @Test
        @DisplayName("Should return all flags for environment")
        void shouldReturnAllFlagsForEnvironment() {
            createTestFlag("readFlag1", "Read flag 1", true);
            createTestFlag("readFlag2", "Read flag 2", false);

            EnvironmentFlagsResponse response = featureFlagService.getFlagsForEnvironment("dev");

            assertEquals("dev", response.getEnvironment());
            assertNotNull(response.getFlags());
            assertEquals(2, response.getFlags().size());
        }

        @Test
        @DisplayName("Should throw EnvironmentNotFoundException for invalid environment")
        void shouldThrowForInvalidEnvironment() {
            assertThrows(EnvironmentNotFoundException.class, () ->
                    featureFlagService.getFlagsForEnvironment("staging"));
        }
    }

    // ──────────────────────────────────────────────
    // Change Log Tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Change History")
    class ChangeLogTests {

        @Test
        @DisplayName("Should record change log on state update")
        void shouldRecordChangeLog() {
            createTestFlag("logTest", "Log test flag", false);

            featureFlagService.updateFlagState("logTest",
                    new UpdateFlagStateRequest("dev", true, 75, "admin"));

            List<ChangeLogResponse> history = featureFlagService.getChangeHistory("logTest");

            assertFalse(history.isEmpty());
            ChangeLogResponse latest = history.get(0);
            assertEquals("logTest", latest.getFlagName());
            assertEquals("dev", latest.getEnvironment());
            assertTrue(latest.isNewEnabled());
            assertEquals(75, latest.getNewRolloutPercentage());
            assertEquals("admin", latest.getChangedBy());
            assertNotNull(latest.getChangedAt());
        }

        @Test
        @DisplayName("Should record multiple changes in order")
        void shouldRecordMultipleChanges() {
            createTestFlag("multiLog", "Multi log test", false);

            featureFlagService.updateFlagState("multiLog",
                    new UpdateFlagStateRequest("dev", true, 50, "admin"));
            featureFlagService.updateFlagState("multiLog",
                    new UpdateFlagStateRequest("dev", false, 0, "deployer"));

            List<ChangeLogResponse> history = featureFlagService.getChangeHistory("multiLog");

            assertTrue(history.size() >= 2);
        }

        @Test
        @DisplayName("Should throw FeatureFlagNotFoundException for unknown flag history")
        void shouldThrowNotFoundForHistory() {
            assertThrows(FeatureFlagNotFoundException.class, () ->
                    featureFlagService.getChangeHistory("nonExistent"));
        }
    }

    // ──────────────────────────────────────────────
    // Environment Tests
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Environments")
    class EnvironmentTests {

        @Test
        @DisplayName("Should return all three environments")
        void shouldReturnAllEnvironments() {
            List<String> environments = featureFlagService.getAllEnvironments();

            assertEquals(3, environments.size());
            assertTrue(environments.contains("dev"));
            assertTrue(environments.contains("test"));
            assertTrue(environments.contains("prod"));
        }
    }
}
