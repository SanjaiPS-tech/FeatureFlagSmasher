package com.FeatureFlagLite.FeartureFlagSmasher.service;

import com.FeatureFlagLite.FeartureFlagSmasher.config.CacheConfig;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.ChangeLogResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.CreateFeatureFlagRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.EnvironmentFlagsResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.FeatureFlagResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.FlagEvaluationResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.FlagStateResponse;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.UpdateFeatureFlagRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.dto.UpdateFlagStateRequest;
import com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog;
import com.FeatureFlagLite.FeartureFlagSmasher.entity.Environment;
import com.FeatureFlagLite.FeartureFlagSmasher.entity.FeatureFlag;
import com.FeatureFlagLite.FeartureFlagSmasher.entity.FlagState;
import com.FeatureFlagLite.FeartureFlagSmasher.exception.DuplicateFeatureFlagException;
import com.FeatureFlagLite.FeartureFlagSmasher.exception.EnvironmentNotFoundException;
import com.FeatureFlagLite.FeartureFlagSmasher.exception.FeatureFlagNotFoundException;
import com.FeatureFlagLite.FeartureFlagSmasher.mapper.EntityMapper;
import com.FeatureFlagLite.FeartureFlagSmasher.repository.ChangeLogRepository;
import com.FeatureFlagLite.FeartureFlagSmasher.repository.EnvironmentRepository;
import com.FeatureFlagLite.FeartureFlagSmasher.repository.FeatureFlagRepository;
import com.FeatureFlagLite.FeartureFlagSmasher.repository.FlagStateRepository;
import com.FeatureFlagLite.FeartureFlagSmasher.util.RolloutEvaluator;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service containing all business logic for feature flag management.
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;
    private final EnvironmentRepository environmentRepository;
    private final FlagStateRepository flagStateRepository;
    private final ChangeLogRepository changeLogRepository;
    private final FlagEventPublisher flagEventPublisher;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,
                               EnvironmentRepository environmentRepository,
                               FlagStateRepository flagStateRepository,
                               ChangeLogRepository changeLogRepository,
                               FlagEventPublisher flagEventPublisher) {
        this.featureFlagRepository = featureFlagRepository;
        this.environmentRepository = environmentRepository;
        this.flagStateRepository = flagStateRepository;
        this.changeLogRepository = changeLogRepository;
        this.flagEventPublisher = flagEventPublisher;
    }

    // ──────────────────────────────────────────────
    // Feature Flag CRUD
    // ──────────────────────────────────────────────

    /**
     * Creates a new feature flag and initializes its state across all environments.
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public FeatureFlagResponse createFeatureFlag(CreateFeatureFlagRequest request) {
        if (featureFlagRepository.existsByName(request.getName())) {
            throw new DuplicateFeatureFlagException(
                    "Feature flag with name '" + request.getName() + "' already exists");
        }

        FeatureFlag featureFlag = new FeatureFlag(
                request.getName(),
                request.getDescription(),
                request.isDefaultState()
        );
        featureFlag = featureFlagRepository.save(featureFlag);

        // Initialize flag state for all environments with the default state
        List<Environment> environments = environmentRepository.findAll();
        int defaultRollout = request.isDefaultState() ? 100 : 0;

        for (Environment environment : environments) {
            FlagState flagState = new FlagState(
                    featureFlag,
                    environment,
                    request.isDefaultState(),
                    defaultRollout
            );
            flagStateRepository.save(flagState);
        }

        log.info("Feature flag created: name={}, defaultState={}", featureFlag.getName(), featureFlag.isDefaultState());
        flagEventPublisher.broadcastFlagChange("FLAG_CREATED", featureFlag.getName(), "*", Map.of(
                "id", featureFlag.getId(),
                "defaultState", featureFlag.isDefaultState()
        ));
        return EntityMapper.toFeatureFlagResponse(featureFlag);
    }

    /**
     * Retrieves a feature flag by its ID.
     */
    @Transactional(readOnly = true)
    public FeatureFlagResponse getFeatureFlagById(Long id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found with id: " + id));
        return EntityMapper.toFeatureFlagResponse(flag);
    }

    /**
     * Retrieves a feature flag by its name.
     */
    @Transactional(readOnly = true)
    public FeatureFlagResponse getFeatureFlagByName(String name) {
        FeatureFlag flag = featureFlagRepository.findByName(name)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found: " + name));
        return EntityMapper.toFeatureFlagResponse(flag);
    }

    /**
     * Retrieves all feature flags.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> getAllFeatureFlags() {
        return featureFlagRepository.findAll().stream()
                .map(EntityMapper::toFeatureFlagResponse)
                .toList();
    }

    /**
     * Updates a feature flag's metadata (name, description, defaultState).
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public FeatureFlagResponse updateFeatureFlag(Long id, UpdateFeatureFlagRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            // Check uniqueness if name is changing
            if (!flag.getName().equals(request.getName()) && featureFlagRepository.existsByName(request.getName())) {
                throw new DuplicateFeatureFlagException(
                        "Feature flag with name '" + request.getName() + "' already exists");
            }
            flag.setName(request.getName());
        }

        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }

        if (request.getDefaultState() != null) {
            flag.setDefaultState(request.getDefaultState());
        }

        flag = featureFlagRepository.save(flag);
        log.info("Feature flag updated: id={}, name={}", flag.getId(), flag.getName());
        flagEventPublisher.broadcastFlagChange("FLAG_UPDATED", flag.getName(), "*", Map.of(
                "id", flag.getId(),
                "defaultState", flag.isDefaultState()
        ));
        return EntityMapper.toFeatureFlagResponse(flag);
    }

    /**
     * Deletes a feature flag and all associated states and change logs.
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public void deleteFeatureFlag(Long id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found with id: " + id));

        changeLogRepository.deleteAllByFeatureFlagId(id);
        flagStateRepository.deleteAllByFeatureFlagId(id);
        featureFlagRepository.delete(flag);

        log.info("Feature flag deleted: id={}, name={}", id, flag.getName());
        flagEventPublisher.broadcastFlagChange("FLAG_DELETED", flag.getName(), "*", Map.of(
                "id", id
        ));
    }

    // ──────────────────────────────────────────────
    // Flag State Management
    // ──────────────────────────────────────────────

    /**
     * Updates the state of a flag within a specific environment.
     * Records the change in the audit log.
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public FlagStateResponse updateFlagState(String flagName, UpdateFlagStateRequest request) {
        FeatureFlag featureFlag = featureFlagRepository.findByName(flagName)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found: " + flagName));

        Environment environment = environmentRepository.findByName(request.getEnvironment())
                .orElseThrow(() -> new EnvironmentNotFoundException(
                        "Environment not found: " + request.getEnvironment()));

        FlagState flagState = flagStateRepository
                .findByFlagNameAndEnvironmentName(flagName, request.getEnvironment())
                .orElse(new FlagState(featureFlag, environment, false, 0));

        // Record the change log before applying updates
        ChangeLog changeLog = new ChangeLog();
        changeLog.setFeatureFlag(featureFlag);
        changeLog.setEnvironment(environment);
        changeLog.setOldEnabled(flagState.getId() != null ? flagState.isEnabled() : null);
        changeLog.setNewEnabled(request.isEnabled());
        changeLog.setOldRolloutPercentage(flagState.getId() != null ? flagState.getRolloutPercentage() : null);
        changeLog.setNewRolloutPercentage(request.getRolloutPercentage());
        changeLog.setChangedBy(request.getChangedBy());
        changeLog.setChangedAt(LocalDateTime.now());
        changeLogRepository.save(changeLog);

        // Apply the state update
        flagState.setEnabled(request.isEnabled());
        flagState.setRolloutPercentage(request.getRolloutPercentage());
        flagState.setUpdatedAt(LocalDateTime.now());
        flagState = flagStateRepository.save(flagState);

        log.info("Flag state changed: flag={}, env={}, enabled={}, rollout={}%, changedBy={}",
                flagName, request.getEnvironment(), request.isEnabled(),
                request.getRolloutPercentage(), request.getChangedBy());

        flagEventPublisher.broadcastFlagChange("STATE_CHANGED", flagName, request.getEnvironment(), Map.of(
                "enabled", request.isEnabled(),
                "rolloutPercentage", request.getRolloutPercentage(),
                "changedBy", request.getChangedBy()
        ));

        return EntityMapper.toFlagStateResponse(flagState);
    }

    /**
     * Gets all flag states for a specific flag across all environments.
     */
    @Transactional(readOnly = true)
    public List<FlagStateResponse> getFlagStates(String flagName) {
        FeatureFlag flag = featureFlagRepository.findByName(flagName)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found: " + flagName));

        return flagStateRepository.findAllByFeatureFlagId(flag.getId()).stream()
                .map(EntityMapper::toFlagStateResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    // Read / Evaluate API
    // ──────────────────────────────────────────────

    /**
     * Retrieves all flag states for a given environment.
     * Cached for performance since this is a frequently called read endpoint.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ENVIRONMENT_FLAGS, key = "#environmentName")
    public EnvironmentFlagsResponse getFlagsForEnvironment(String environmentName) {
        if (!environmentRepository.existsByName(environmentName)) {
            throw new EnvironmentNotFoundException("Environment not found: " + environmentName);
        }

        List<FlagState> flagStates = flagStateRepository.findAllByEnvironmentName(environmentName);
        Map<String, Boolean> flags = new LinkedHashMap<>();

        for (FlagState state : flagStates) {
            flags.put(state.getFeatureFlag().getName(), state.isEnabled());
        }

        log.debug("Retrieved flags for environment: {}, count={}", environmentName, flags.size());
        return new EnvironmentFlagsResponse(environmentName, flags);
    }

    /**
     * Evaluates a flag for a specific user using deterministic percentage rollout.
     * Cached for performance.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_FLAG_STATES, key = "#flagName + ':' + #environmentName + ':' + #userId")
    public FlagEvaluationResponse evaluateFlag(String flagName, String environmentName, String userId) {
        if (!environmentRepository.existsByName(environmentName)) {
            throw new EnvironmentNotFoundException("Environment not found: " + environmentName);
        }

        FlagState flagState = flagStateRepository.findByFlagNameAndEnvironmentName(flagName, environmentName)
                .orElseThrow(() -> new FeatureFlagNotFoundException(
                        "Flag state not found for flag '" + flagName + "' in environment '" + environmentName + "'"));

        boolean isEnabled = flagState.isEnabled();

        int rolloutPercentage = flagState.getRolloutPercentage();
        int bucket = (userId != null && !userId.isBlank()) ? RolloutEvaluator.getBucket(flagName, userId) : 0;

        // Apply percentage rollout if userId is provided and rollout is between 0-100%
        if (isEnabled && userId != null && !userId.isBlank()) {
            isEnabled = RolloutEvaluator.isUserInRollout(flagName, userId, rolloutPercentage);
        }

        String explanation;
        if (!flagState.isEnabled()) {
            explanation = "Feature is turned off for " + environmentName.toUpperCase() + ". All users receive the fallback baseline experience.";
        } else if (userId == null || userId.isBlank()) {
            explanation = "Evaluated against general environment state (no specific user supplied). State is active.";
        } else if (rolloutPercentage >= 100) {
            explanation = "Active for 100% of traffic. Every user in " + environmentName.toUpperCase() + " receives this feature.";
        } else if (rolloutPercentage <= 0) {
            explanation = "Rollout is set to 0%. Feature is safely held in reserve.";
        } else if (isEnabled) {
            explanation = "User '" + userId + "' mapped to hash slot " + bucket + " (within the " + rolloutPercentage + "% rollout boundary: 0-" + (rolloutPercentage - 1) + "). Granted access.";
        } else {
            explanation = "User '" + userId + "' mapped to hash slot " + bucket + " (above the " + rolloutPercentage + "% rollout boundary: " + rolloutPercentage + "-99). Retained on standard fallback.";
        }

        log.debug("Flag evaluated: flag={}, env={}, userId={}, enabled={}, bucket={}",
                flagName, environmentName, userId, isEnabled, bucket);

        return new FlagEvaluationResponse(
                flagName,
                environmentName,
                isEnabled,
                rolloutPercentage,
                bucket,
                explanation
        );
    }

    /**
     * Evaluates all registered feature flags for a specific user in an environment.
     */
    @Transactional(readOnly = true)
    public List<FlagEvaluationResponse> evaluateAllFlagsForUser(String environmentName, String userId) {
        if (!environmentRepository.existsByName(environmentName)) {
            throw new EnvironmentNotFoundException("Environment not found: " + environmentName);
        }

        List<FeatureFlag> flags = featureFlagRepository.findAll();
        return flags.stream()
                .map(flag -> evaluateFlag(flag.getName(), environmentName, userId))
                .toList();
    }

    /**
     * Promotes and syncs all flag configurations from a source environment to a target environment.
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public int syncEnvironmentStates(String sourceEnv, String targetEnv, String changedBy) {
        if (!environmentRepository.existsByName(sourceEnv)) {
            throw new EnvironmentNotFoundException("Source environment not found: " + sourceEnv);
        }
        if (!environmentRepository.existsByName(targetEnv)) {
            throw new EnvironmentNotFoundException("Target environment not found: " + targetEnv);
        }

        List<FlagState> sourceStates = flagStateRepository.findAllByEnvironmentName(sourceEnv);
        int syncedCount = 0;

        for (FlagState src : sourceStates) {
            String flagName = src.getFeatureFlag().getName();
            FlagState targetState = flagStateRepository.findByFlagNameAndEnvironmentName(flagName, targetEnv)
                    .orElse(null);

            if (targetState != null) {
                boolean oldEnabled = targetState.isEnabled();
                int oldRollout = targetState.getRolloutPercentage();

                targetState.setEnabled(src.isEnabled());
                targetState.setRolloutPercentage(src.getRolloutPercentage());
                flagStateRepository.save(targetState);

                // Record audit log
                com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog logEntry =
                        new com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog(
                                targetState.getFeatureFlag(),
                                targetState.getEnvironment(),
                                oldEnabled,
                                src.isEnabled(),
                                oldRollout,
                                src.getRolloutPercentage(),
                                changedBy != null ? changedBy : "admin"
                        );
                changeLogRepository.save(logEntry);
                syncedCount++;
            }
        }

        log.info("Synced {} flags from {} to {} by {}", syncedCount, sourceEnv, targetEnv, changedBy);
        return syncedCount;
    }

    /**
     * Emergency Killswitch: Disables all flags in an environment.
     */
    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public int emergencyKillswitch(String environmentName, String changedBy) {
        if (!environmentRepository.existsByName(environmentName)) {
            throw new EnvironmentNotFoundException("Environment not found: " + environmentName);
        }

        List<FlagState> states = flagStateRepository.findAllByEnvironmentName(environmentName);
        int pausedCount = 0;

        for (FlagState state : states) {
            if (state.isEnabled()) {
                boolean oldEnabled = state.isEnabled();
                int oldRollout = state.getRolloutPercentage();

                state.setEnabled(false);
                state.setRolloutPercentage(0);
                flagStateRepository.save(state);

                com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog logEntry =
                        new com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog(
                                state.getFeatureFlag(),
                                state.getEnvironment(),
                                oldEnabled,
                                false,
                                oldRollout,
                                0,
                                changedBy != null ? changedBy : "admin-killswitch"
                        );
                changeLogRepository.save(logEntry);
                pausedCount++;
            }
        }

        log.warn("Emergency Killswitch triggered for environment {}: {} flags paused by {}",
                environmentName, pausedCount, changedBy);
        return pausedCount;
    }

    // ──────────────────────────────────────────────
    // Change History
    // ──────────────────────────────────────────────

    /**
     * Retrieves the change history for a specific flag.
     */
    @Transactional(readOnly = true)
    public List<ChangeLogResponse> getChangeHistory(String flagName) {
        if (!featureFlagRepository.existsByName(flagName)) {
            throw new FeatureFlagNotFoundException("Feature flag not found: " + flagName);
        }

        return changeLogRepository.findAllByFeatureFlagName(flagName).stream()
                .map(EntityMapper::toChangeLogResponse)
                .toList();
    }

    /**
     * Retrieves all change history across the entire system.
     */
    @Transactional(readOnly = true)
    public List<ChangeLogResponse> getAllChangeHistory() {
        return changeLogRepository.findAllRecentLogs().stream()
                .map(EntityMapper::toChangeLogResponse)
                .toList();
    }

    /**
     * Invalidate all in-memory caches.
     */
    @CacheEvict(value = {CacheConfig.CACHE_ENVIRONMENT_FLAGS, CacheConfig.CACHE_FLAG_STATES}, allEntries = true)
    public void purgeAllCaches() {
        log.info("Administrative cache purge executed: all in-memory caches evicted.");
        flagEventPublisher.broadcastFlagChange("CACHE_PURGED", "*", "*", Map.of());
    }

    // ──────────────────────────────────────────────
    // Environments
    // ──────────────────────────────────────────────

    /**
     * Returns all available environments.
     */
    @Transactional(readOnly = true)
    public List<String> getAllEnvironments() {
        return environmentRepository.findAll().stream()
                .map(Environment::getName)
                .toList();
    }
}
