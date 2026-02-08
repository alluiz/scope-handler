package com.company.scopehandler.api.config;

import java.util.Objects;

public final class AuthorizationServerSettings {
    private final String name;
    private final String environment;
    private final String baseUrl;
    private final String username;
    private final String password;

    private AuthorizationServerSettings(String name,
                                        String environment,
                                        String baseUrl,
                                        String username,
                                        String password) {
        this.name = Objects.requireNonNull(name, "name");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
    }

    public static AuthorizationServerSettings from(AppConfig config, String name, String environment) {
        String baseUrl = config.getRequired("as." + name + ".env." + environment + ".baseUrl");
        String username = config.getRequired(resolveKey(config, name, "auth.username"));
        String password = config.getRequired(resolveKey(config, name, "auth.password"));

        return new AuthorizationServerSettings(
                name,
                environment,
                baseUrl,
                username,
                password
        );
    }

    private static String resolveKey(AppConfig config, String name, String suffix) {
        String specificKey = "as." + name + "." + suffix;
        if (config.has(specificKey)) {
            return specificKey;
        }
        return "as." + name + "." + suffix;
    }

    public String getName() {
        return name;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
