package com.featureflaglite.featureflagsmasher.application.service;

import com.featureflaglite.featureflagsmasher.application.mapper.EntityMapper;
import com.featureflaglite.featureflagsmasher.application.query.GetAllFeatureFlagsQuery;
import com.featureflaglite.featureflagsmasher.application.query.GetFeatureFlagByIdQuery;
import com.featureflaglite.featureflagsmasher.application.query.GetFeatureFlagByNameQuery;
import com.featureflaglite.featureflagsmasher.application.query.GetFlagStatesQuery;
import com.featureflaglite.featureflagsmasher.application.command.CreateFeatureFlagCommand;
import com.featureflaglite.featureflagsmasher.application.command.DeleteFeatureFlagCommand;
import com.featureflaglite.featureflagsmasher.application.command.UpdateFeatureFlagCommand;
import com.featureflaglite.featureflagsmasher.application.command.UpdateFlagStateCommand;
import com.featureflaglite.featureflagsmasher.domain.model.ChangeLog;
import com.featureflaglite.featureflagsmasher.domain.model.Environment;
import com.featureflaglite.featureflagsmasher.domain.model.FeatureFlag;
import com.featureflaglite.featureflagsmasher.domain.model.FlagState;
import com.featureflaglite.featureflagsmasher.domain.repository.ChangeLogRepository;
import com.featureflaglite.featureflagsmasher.domain.repository.EnvironmentRepository;
import com.featureflaglite.featureflagsmasher.domain.repository.FeatureFlagRepository;
import com.featureflaglite.featureflagsmasher.domain.repository.FlagStateRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Application service containing all business logic for feature flag management.
 * This is the use case layer that orchestrates domain objects and repositories.
 */
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final EnvironmentRepository environmentRepository;
    private final FlagStateRepository flagStateRepository;
    private final ChangeLogRepository changeLogRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,
                              EnvironmentRepository environmentRepository,
                              FlagStateRepository flagStateRepository,
                              ChangeLogRepository changeLogRepository) {
        this.featureFlagRepository = Objects.requireNonNull(featureFlagRepository);
        this.environmentRepository = Objects.requireNonNull(environmentRepository);
        this.flagStateRepository = Objects.requireNonNull(flagStateRepository);
        this.changeLogRepository = Objects.requireNonNull(changeLogRepository);
    }

    // Feature Flag CRUD operations
    
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

        return EntityMapper.toFeatureFlagResponse(featureFlag);
    }

    public FeatureFlagResponse getFeatureFlagById(Long id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found with id: " + id));
        return EntityMapper.toFeatureFlagResponse(flag);
    }

    public FeatureFlagResponse getFeatureFlagByName(String name) {
        FeatureFlag flag = featureFlagRepository.findByName(name)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found: " + name));
        return EntityMapper.toFeatureFlagResponse(flag);
    }

    public List<FeatureFlagResponse> getAllFeatureFlags() {
        return featureFlagRepository.findAll().stream()
                .map(EntityMapper::toFeatureFlagResponse)
                .collect(Collectors.toList());
    }

    public FeatureFlagResponse updateFeatureFlag(Long id, UpdateFeatureFlagRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            // Check uniqueness if name is changing
            if (!flag.getName().equals(request.getName()) && 
                    featureFlagRepository.existsByName(request.getName())) {
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
        return EntityMapper.toFeatureFlagResponse(flag);
    }

    public void deleteFeatureFlag(Long id) {
        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found with id: " + id));

        changeLogRepository.deleteAllByFeatureFlagId(id);
        flagStateRepository.deleteAllByFeatureFlagId(id);
        featureFlagRepository.delete(flag);
    }

    // Flag State Management
    
    public FlagStateResponse updateFlagState(String flagName, UpdateFlagStateRequest request) {
        FeatureFlag featureFlag = featureFlagRepository.findByName(flagName)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found: " + flagName));

        Environment environment = environmentRepository.findByName(request.getEnvironment())
                .orElseThrow(() -> new EnvironmentNotFoundException(
                        "Environment not found: " + request.getEnvironment()));

        FlagState flagState = flagStateRepository.findByFlagNameAndEnvironmentName(
                flagName, request.getEnvironment())
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

        return EntityMapper.toFlagStateResponse(flagState);
    }

    public List<FlagStateResponse> getFlagStates(String flagName) {
        FeatureFlag flag = featureFlagRepository.findByName(flagName)
                .orElseThrow(() -> new FeatureFlagNotFoundException("Feature flag not found: " + flagName));

        return flagStateRepository.findAllByFeatureFlagId(flag.getId()).stream()
                .map(EntityMapper::toFlagStateResponse)
                .collect(Collectors.toList());
    }

    // Read / Evaluate API
    
    public EnvironmentFlagsResponse getFlagsForEnvironment(String environmentName) {
        if (!environmentRepository.existsByName(environmentName)) {
            throw new EnvironmentNotFoundException("Environment not found: " + environmentName);
        }

        List<FlagState> flagStates = flagStateRepository.findAllByEnvironmentName(environmentName);
        Map<String, Boolean> flags = new LinkedHashMap<>();

        for (FlagState state : flagStates) {
            flags.put(state.getFeatureFlag().getName(), state.isEnabled());
        }

        return new EnvironmentFlagsResponse(environmentName, flags);
    }

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
            explanation = "Feature is turned off for " + environmentName.toUpperCase() + 
                    ". All users receive the fallback baseline experience.";
        } else if (userId == null || userId.isBlank()) {
            explanation = "Evaluated against general environment state (no specific user supplied). State is active.";
        } else if (rolloutPercentage >= 100) {
            explanation = "Active for 100% of traffic. Every user in " + environmentName.toUpperCase() + 
                    " receives this feature.";
        } else if (rolloutPercentage <= 0) {
            explanation = "Rollout is set to 0%. Feature is safely held in reserve.";
        } else if (isEnabled) {
            explanation = "User '" + userId + "' mapped to hash slot " + bucket + 
                    " (within the " + rolloutPercentage + "% rollout boundary: 0-" + (rolloutPercentage - 1) + 
                    "). Granted access.";
        } else {
            explanation = "User '" + userId + "' mapped to hash slot " + bucket + 
                    " (above the " + rolloutPercentage + "% rollout boundary: " + rolloutPercentage + "-99). " +
                    "Retained on standard fallback.";
        }

        return new FlagEvaluationResponse(
                flagName,
                environmentName,
                isEnabled,
                rolloutPercentage,
                bucket,
                explanation
        );
    }

    public List<FlagEvaluationResponse> evaluateAllFlagsForUser(String environmentName, String userId) {
        if (!environmentRepository.existsByName(environmentName)) {
            throw new EnvironmentNotFoundException("Environment not found: " + environmentName);
        }

        List<FeatureFlag> flags = featureFlagRepository.findAll();
        return flags.stream()
                .map(flag -> evaluateFlag(flag.getName(), environmentName, userId))
                .collect(Collectors.toList());
    }

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
                ChangeLog logEntry = new ChangeLog(
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

        return syncedCount;
    }

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

                ChangeLog logEntry = new ChangeLog(
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

        return pausedCount;
    }

    // Change History
    
    public List<ChangeLogResponse> getChangeHistory(String flagName) {
        if (!featureFlagRepository.existsByName(flagName)) {
            throw new FeatureFlagNotFoundException("Feature flag not found: " + flagName);
        }

        return changeLogRepository.findAllByFeatureFlagName(flagName).stream()
                .map(EntityMapper::toChangeLogResponse)
                .collect(Collectors.toList());
    }

    public List<ChangeLogResponse> getAllChangeHistory() {
        return changeLogRepository.findAllRecentLogs().stream()
                .map(EntityMapper::toChangeLogResponse)
                .collect(Collectors.toList());
    }

    // Environments
    
    public List<String> getAllEnvironments() {
        return environmentRepository.findAll().stream()
                .map(Environment::getName)
                .collect(Collectors.toList());
    }
}