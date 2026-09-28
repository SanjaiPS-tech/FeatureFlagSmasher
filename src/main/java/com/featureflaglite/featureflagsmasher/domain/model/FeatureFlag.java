package com.featureflaglite.featureflagsmasher.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a feature flag.
 * Pure domain model with no framework dependencies.
 */
public class FeatureFlag {

    private Long id;
    private String name;
    private String description;
    private boolean defaultState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected FeatureFlag() {
        // For JPA
    }

    public FeatureFlag(String name, String description, boolean defaultState) {
        this.name = Objects.requireNonNull(name, "Flag name cannot be null");
        this.description = description;
        this.defaultState = defaultState;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Flag name cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDefaultState() {
        return defaultState;
    }

    public void setDefaultState(boolean defaultState) {
        this.defaultState = defaultState;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlag that = (FeatureFlag) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "FeatureFlag{id=" + id + ", name='" + name + "', defaultState=" + defaultState + "}";
    }
}