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

        seedSampleFlags();
    }

    private void seedSampleFlags() {
        var devEnv = environmentRepository.findByName("dev").orElseThrow();
        var testEnv = environmentRepository.findByName("test").orElseThrow();
        var prodEnv = environmentRepository.findByName("prod").orElseThrow();

        // 1. newDashboard: dev=ON (100%), test=ON (100%), prod=OFF (0%)
        createSeedFlag(
                "newDashboard",
                "Next-generation analytics telemetry dashboard with live charts and real-time KPI metrics",
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
                "AI semantic search with vector indexing and instant query suggestions",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 50),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 5. autoSaveDrafts: dev=ON (100%), test=ON (100%), prod=ON (100%)
        createSeedFlag(
                "autoSaveDrafts",
                "Continuously persists form and document edits to cloud storage every 3 seconds",
                true,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 100),
                        new SeedState(prodEnv, true, 100)
                )
        );

        // 6. twoFactorAuth: dev=ON (100%), test=ON (100%), prod=ON (75%)
        createSeedFlag(
                "twoFactorAuth",
                "Mandatory hardware security key (FIDO2/WebAuthn) and TOTP multi-factor verification",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 100),
                        new SeedState(prodEnv, true, 75)
                )
        );

        // 7. exportToPdf: dev=ON (100%), test=ON (50%), prod=OFF (0%)
        createSeedFlag(
                "exportToPdf",
                "High-resolution vector PDF export engine for invoices, dashboards, and audit logs",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 50),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 8. aiTelemetrySummarizer: dev=ON (50%), test=OFF (0%), prod=OFF (0%)
        createSeedFlag(
                "aiTelemetrySummarizer",
                "Generative AI incident summaries and automated root cause analysis",
                false,
                List.of(
                        new SeedState(devEnv, true, 50),
                        new SeedState(testEnv, false, 0),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 9. rateLimitStrictV2: dev=ON (100%), test=ON (100%), prod=ON (10%)
        createSeedFlag(
                "rateLimitStrictV2",
                "Enhanced token-bucket API rate limiting with proactive DDoS protection",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 100),
                        new SeedState(prodEnv, true, 10)
                )
        );

        // 10. realtimeCollaboration: dev=ON (30%), test=OFF (0%), prod=OFF (0%)
        createSeedFlag(
                "realtimeCollaboration",
                "Multi-user live cursor presence and collaborative canvas editing",
                false,
                List.of(
                        new SeedState(devEnv, true, 30),
                        new SeedState(testEnv, false, 0),
                        new SeedState(prodEnv, false, 0)
                )
        );

        // 11. ssoEnterpriseOkta: dev=ON (100%), test=ON (100%), prod=ON (100%)
        createSeedFlag(
                "ssoEnterpriseOkta",
                "SAML 2.0 and OIDC single sign-on integration for Enterprise organizations",
                true,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 100),
                        new SeedState(prodEnv, true, 100)
                )
        );

        // 12. biometricFaceUnlock: dev=ON (100%), test=ON (50%), prod=OFF (0%)
        createSeedFlag(
                "biometricFaceUnlock",
                "Native WebAuthn biometric facial and fingerprint authentication",
                false,
                List.of(
                        new SeedState(devEnv, true, 100),
                        new SeedState(testEnv, true, 50),
                        new SeedState(prodEnv, false, 0)
                )
        );

        log.info("Feature flags catalog verification and seeding complete.");
    }

    private void createSeedFlag(String name, String description, boolean defaultState, List<SeedState> states) {
        if (featureFlagRepository.existsByName(name)) {
            return;
        }

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
