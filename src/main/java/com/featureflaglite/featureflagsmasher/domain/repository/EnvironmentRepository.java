package com.featureflaglite.featureflagsmasher.domain.repository;

import com.featureflaglite.featureflagsmasher.domain.model.Environment;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Environment.
 */
public interface EnvironmentRepository {

    Optional<Environment> findById(Long id);

    Optional<Environment> findByName(String name);

    List<Environment> findAll();

    boolean existsByName(String name);

    Environment save(Environment environment);

    void delete(Environment environment);

    void deleteById(Long id);
}