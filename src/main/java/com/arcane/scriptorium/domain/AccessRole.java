package com.arcane.scriptorium.domain;

public enum AccessRole {
    COMMON_READER("Leitor", "LR"),
    CRITICAL_READER("Leitor critico", "LC"),
    WRITER("Escritor", "ES");

    private final String displayName;
    private final String token;

    AccessRole(String displayName, String token) {
        this.displayName = displayName;
        this.token = token;
    }

    public String displayName() {
        return displayName;
    }

    public String token() {
        return token;
    }

    public boolean isReader() {
        return this == COMMON_READER || this == CRITICAL_READER;
    }

    public boolean isWriter() {
        return this == WRITER;
    }
}
