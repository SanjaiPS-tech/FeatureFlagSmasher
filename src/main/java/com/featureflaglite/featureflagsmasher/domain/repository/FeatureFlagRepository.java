package com.featureflaglite.featureflagsmasher.domain.repository;

import com.featureflaglite.featureflagsmasher.domain.model.FeatureFlag;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for FeatureFlag.
 * Defines the contract without any framework dependencies.
 */
public interface FeatureFlagRepository {

    Optional<FeatureFlag> findById(Long id);

    Optional<FeatureFlag> findByName(String name);

    List<FeatureFlag> findAll();

    boolean existsByName(String name);

    FeatureFlag save(FeatureFlag featureFlag);

    void delete(FeatureFlag featureFlag);

    void deleteById(Long id);
}