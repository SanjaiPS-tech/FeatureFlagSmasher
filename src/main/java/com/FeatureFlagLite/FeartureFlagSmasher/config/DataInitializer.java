package com.FeatureFlagLite.FeartureFlagSmasher.config;

import com.FeatureFlagLite.FeartureFlagSmasher.entity.Environment;
import com.FeatureFlagLite.FeartureFlagSmasher.repository.EnvironmentRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with the three supported environments on startup.
 * Only inserts environments that don't already exist.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<String> SUPPORTED_ENVIRONMENTS = List.of("dev", "test", "prod");

    private final EnvironmentRepository environmentRepository;
    private final com.FeatureFlagLite.FeartureFlagSmasher.repository.FeatureFlagRepository featureFlagRepository;
    private final com.FeatureFlagLite.FeartureFlagSmasher.repository.FlagStateRepository flagStateRepository;
    private final com.FeatureFlagLite.FeartureFlagSmasher.repository.ChangeLogRepository changeLogRepository;

    public DataInitializer(EnvironmentRepository environmentRepository,
                           com.FeatureFlagLite.FeartureFlagSmasher.repository.FeatureFlagRepository featureFlagRepository,
                           com.FeatureFlagLite.FeartureFlagSmasher.repository.FlagStateRepository flagStateRepository,
                           com.FeatureFlagLite.FeartureFlagSmasher.repository.ChangeLogRepository changeLogRepository) {
        this.environmentRepository = environmentRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.flagStateRepository = flagStateRepository;
        this.changeLogRepository = changeLogRepository;
    }

    @Override
    public void run(String... args) {
        for (String envName : SUPPORTED_ENVIRONMENTS) {
            if (!environmentRepository.existsByName(envName)) {
                environmentRepository.save(new Environment(envName));
                log.info("Initialized environment: {}", envName);
            }
        }
        log.info("Environment initialization complete. Environments: {}", SUPPORTED_ENVIRONMENTS);

        // Seed demo feature flags if database is empty
        if (featureFlagRepository.count() == 0) {
            seedSampleFlags();
        }
    }

    private void seedSampleFlags() {
        log.info("Seeding sample feature flags for demo website...");
        var devEnv = environmentRepository.findByName("dev").orElseThrow();
        var testEnv = environmentRepository.findByName("test").orElseThrow();
        var prodEnv = environmentRepository.findByName("prod").orElseThrow();

        // 1. newDashboard: dev=ON (100%), test=ON (100%), prod=OFF (0%)
        createSeedFlag(
                "newDashboard",
                "Next-generation analytics dashboard with live charts and real-time KPI metrics",
                true,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 100),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 2. darkMode: dev=ON (100%), test=OFF (0%), prod=OFF (0%)
        createSeedFlag(
                "darkMode",
                "Deep OLED dark theme and customizable contrast color scheme",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, false, 0),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 3. newCheckout: dev=ON (50%), test=ON (25%), prod=OFF (0%)
        createSeedFlag(
                "newCheckout",
                "Express 1-click checkout with streamlined payment processing",
                false,
                List.of(
                        new SeedState(devEnv, true, 50),
                        new SeedState(testEnv, true, 25),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 4. betaSearch: dev=ON (100%), test=ON (50%), prod=OFF (0%)
        createSeedFlag(
                "betaSearch",
                "AI semantic search with instant fuzzy matching and query suggestions",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 50),
                        new SeedState(prodEnv, false, 0)
                )
        );

        log.info("Sample feature flags seeded successfully.");
    }

    private void createSeedFlag(String name, String description, boolean defaultState, List<SeedState> states) {
        var flag = featureFlagRepository.save(new com.FeatureFlagLite.FeartureFlagSmasher.entity.FeatureFlag(
                name, description, defaultState
        ));

        for (var s : states) {
            flagStateRepository.save(new com.FeatureFlagLite.FeartureFlagSmasher.entity.FlagState(
                    flag, s.env, s.enabled, s.rollout
            ));
            changeLogRepository.save(new com.FeatureFlagLite.FeartureFlagSmasher.entity.ChangeLog(
                    flag, s.env, null, s.enabled, null, s.rollout, "system-init"
            ));
        }
    }

    private record SeedState(Environment env, boolean enabled, int rollout) {}
}
