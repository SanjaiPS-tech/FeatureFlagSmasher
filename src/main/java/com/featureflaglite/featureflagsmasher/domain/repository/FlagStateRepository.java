package com.featureflaglite.featureflagsmasher.domain.repository;

import com.featureflaglite.featureflagsmasher.domain.model.FlagState;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for FlagState.
 */
public interface FlagStateRepository {

    Optional<FlagState> findById(Long id);

    Optional<FlagState> findByFlagNameAndEnvironmentName(String flagName, String environmentName);

    List<FlagState> findAllByEnvironmentName(String environmentName);

    List<FlagState> findAllByFeatureFlagId(Long featureFlagId);

    FlagState save(FlagState flagState);

    void delete(FlagState flagState);

    void deleteById(Long id);

    void deleteAllByFeatureFlagId(Long featureFlagId);
}