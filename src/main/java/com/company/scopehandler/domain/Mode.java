package com.company.scopehandler.domain;

public enum Mode {
    ASSOCIATE,
    DISSOCIATE;

    public static Mode from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("mode is required");
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "associate", "associar" -> ASSOCIATE;
            case "dissociate", "desassociar" -> DISSOCIATE;
            default -> throw new IllegalArgumentException("invalid mode: " + value);
        };
    }
}
