package com.featureflaglite.featureflagsmasher.repository;

import com.featureflaglite.featureflagsmasher.entity.Environment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    Optional<Environment> findByName(String name);

    boolean existsByName(String name);
}
