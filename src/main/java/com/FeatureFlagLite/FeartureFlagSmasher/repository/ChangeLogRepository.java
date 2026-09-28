package com.FeatureFlagLite.FeartureFlagSmasher.repository;

import com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

    @Query("SELECT cl FROM ChangeLog cl " +
           "JOIN FETCH cl.featureFlag " +
           "JOIN FETCH cl.environment " +
           "ORDER BY cl.changedAt DESC")
    List<ChangeLog> findAllRecentLogs();

    @Query("SELECT cl FROM ChangeLog cl " +
           "JOIN FETCH cl.featureFlag " +
           "JOIN FETCH cl.environment " +
           "WHERE cl.featureFlag.name = :flagName " +
           "ORDER BY cl.changedAt DESC")
    List<ChangeLog> findAllByFeatureFlagName(@Param("flagName") String flagName);

    @Query(value = "SELECT cl FROM ChangeLog cl " +
                   "JOIN FETCH cl.featureFlag " +
                   "JOIN FETCH cl.environment " +
                   "WHERE cl.featureFlag.name = :flagName",
           countQuery = "SELECT COUNT(cl) FROM ChangeLog cl WHERE cl.featureFlag.name = :flagName")
    Page<ChangeLog> findAllByFeatureFlagNamePaged(@Param("flagName") String flagName, Pageable pageable);

    void deleteAllByFeatureFlagId(Long featureFlagId);
}
