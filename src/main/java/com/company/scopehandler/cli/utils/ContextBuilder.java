package com.company.scopehandler.cli.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ContextBuilder {
    private final Map<String, String> values = new LinkedHashMap<>();

    public ContextBuilder put(String key, String value) {
        if (key != null && value != null) {
            values.put(key, value);
        }
        return this;
    }

    public Map<String, String> build() {
        return values;
    }
}
