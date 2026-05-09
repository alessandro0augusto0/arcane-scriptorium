package com.arcane.scriptorium.domain;

import java.util.Objects;

public record ProcessDescriptor(int id, String name, AccessRole role) {
    public ProcessDescriptor {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(role, "role");
    }

    public String shortName() {
        return "%s #%d".formatted(name, id);
    }

    public String label() {
        return "[%s] %s".formatted(role.token(), shortName());
    }
}
