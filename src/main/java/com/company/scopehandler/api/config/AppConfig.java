package com.company.scopehandler.api.config;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class AppConfig {
    private final Properties properties;

    public AppConfig(Properties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required config: " + key);
        }
        return value.trim();
    }

    public String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    public boolean has(String key) {
        String value = properties.getProperty(key);
        return value != null && !value.isBlank();
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public Path getPath(String key, String defaultValue) {
        return Path.of(get(key, defaultValue));
    }

    public Properties raw() {
        return properties;
    }

    public static final class Builder {
        private final Properties properties = new Properties();

        public Builder fromProperties(Properties props) {
            if (props != null) {
                properties.putAll(props);
            }
            return this;
        }

        public Builder set(String key, String value) {
            if (key != null && value != null) {
                properties.setProperty(key, value);
            }
            return this;
        }

        public AppConfig build() {
            return new AppConfig(properties);
        }
    }
}
