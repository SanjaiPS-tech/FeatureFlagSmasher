package com.featureflaglite.featureflagsmasher.domain.model;

import java.util.Objects;

/**
 * Domain entity representing a deployment environment.
 */
public class Environment {

    private Long id;
    private String name;

    protected Environment() {
        // For JPA
    }

    public Environment(String name) {
        this.name = Objects.requireNonNull(name, "Environment name cannot be null");
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
        this.name = Objects.requireNonNull(name, "Environment name cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Environment that = (Environment) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Environment{id=" + id + ", name='" + name + "'}";
    }
}