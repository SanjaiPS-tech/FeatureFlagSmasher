-- FeatureFlagLite Database Schema
-- MySQL 8.x compatible

CREATE DATABASE IF NOT EXISTS featureflaglite
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE featureflaglite;

-- ──────────────────────────────────────────────
-- Feature Flags Table
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS feature_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    default_state BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_feature_flags_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ──────────────────────────────────────────────
-- Environments Table
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS environments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_environments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default environments
INSERT IGNORE INTO environments (name) VALUES ('dev'), ('test'), ('prod');

-- ──────────────────────────────────────────────
-- Flag States Table
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS flag_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feature_flag_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_percentage INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_flag_env UNIQUE (feature_flag_id, environment_id),
    CONSTRAINT fk_flag_states_flag FOREIGN KEY (feature_flag_id) REFERENCES feature_flags(id) ON DELETE CASCADE,
    CONSTRAINT fk_flag_states_env FOREIGN KEY (environment_id) REFERENCES environments(id) ON DELETE CASCADE,
    CONSTRAINT chk_rollout_percentage CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_flag_states_env ON flag_states(environment_id);
CREATE INDEX idx_flag_states_flag ON flag_states(feature_flag_id);

-- ──────────────────────────────────────────────
-- Change Logs Table
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feature_flag_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    old_enabled BOOLEAN,
    new_enabled BOOLEAN NOT NULL,
    old_rollout_percentage INT,
    new_rollout_percentage INT NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_change_logs_flag FOREIGN KEY (feature_flag_id) REFERENCES feature_flags(id) ON DELETE CASCADE,
    CONSTRAINT fk_change_logs_env FOREIGN KEY (environment_id) REFERENCES environments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_change_logs_flag ON change_logs(feature_flag_id);
CREATE INDEX idx_change_logs_changed_at ON change_logs(changed_at);
