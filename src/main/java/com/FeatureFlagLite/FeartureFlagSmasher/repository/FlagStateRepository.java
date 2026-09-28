package com.FeatureFlagLite.FeartureFlagSmasher.repository;

import com.FeatureFlagLite.FeartureFlagSmasher.entity.FlagState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlagStateRepository extends JpaRepository<FlagState, Long> {

    @Query("SELECT fs FROM FlagState fs " +
           "JOIN FETCH fs.featureFlag " +
           "JOIN FETCH fs.environment " +
           "WHERE fs.environment.name = :environmentName")
    List<FlagState> findAllByEnvironmentName(@Param("environmentName") String environmentName);

    @Query("SELECT fs FROM FlagState fs " +
           "JOIN FETCH fs.featureFlag " +
           "JOIN FETCH fs.environment " +
           "WHERE fs.featureFlag.name = :flagName AND fs.environment.name = :environmentName")
    Optional<FlagState> findByFlagNameAndEnvironmentName(
            @Param("flagName") String flagName,
            @Param("environmentName") String environmentName);

    @Query("SELECT fs FROM FlagState fs " +
           "JOIN FETCH fs.featureFlag " +
           "JOIN FETCH fs.environment " +
           "WHERE fs.featureFlag.id = :featureFlagId")
    List<FlagState> findAllByFeatureFlagId(@Param("featureFlagId") Long featureFlagId);

    void deleteAllByFeatureFlagId(Long featureFlagId);
}
