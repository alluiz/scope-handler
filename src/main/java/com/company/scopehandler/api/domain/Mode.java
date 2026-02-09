package com.company.scopehandler.api.domain;

public enum Mode {
    ADD,
    REMOVE,
    LIST,
    FIND;

    public static Mode from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("mode is required");
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "add", "associar", "associate" -> ADD;
            case "remove", "desassociar", "dissociate" -> REMOVE;
            case "list", "listar" -> LIST;
            case "find", "buscar" -> FIND;
            default -> throw new IllegalArgumentException("invalid mode: " + value);
        };
    }
}
