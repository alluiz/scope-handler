package com.company.scopehandler.api.domain;

public enum FindMatchMode {
    AND,
    OR;

    public static FindMatchMode from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("find-mode is required");
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "and", "all" -> AND;
            case "or", "any" -> OR;
            default -> throw new IllegalArgumentException("invalid find-mode: " + value);
        };
    }
}
