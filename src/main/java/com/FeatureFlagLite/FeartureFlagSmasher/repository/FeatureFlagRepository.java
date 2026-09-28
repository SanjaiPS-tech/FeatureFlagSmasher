package com.FeatureFlagLite.FeartureFlagSmasher.repository;

import com.FeatureFlagLite.FeartureFlagSmasher.entity.FeatureFlag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    Optional<FeatureFlag> findByName(String name);

    boolean existsByName(String name);
}
