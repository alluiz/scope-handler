package com.company.scopehandler.services;

import com.company.scopehandler.config.AppConfig;
import com.company.scopehandler.config.AuthorizationServerSettings;
import com.company.scopehandler.ports.AuthorizationServerClient;
import com.company.scopehandler.axway.AxwayAuthorizationServerClient;
import com.company.scopehandler.axway.AxwayRequestLogger;
import com.company.scopehandler.axway.cache.AxwayCacheStore;
import java.nio.file.Path;
import java.time.Duration;

public final class AuthorizationServerFactory {
    public AuthorizationServerClient create(String asName, String environment, AppConfig config, Path cacheDir) {
        AuthorizationServerSettings settings = AuthorizationServerSettings.from(config, asName, environment);
        return switch (asName) {
            case "mock" -> new MockAuthorizationServerClient(settings);
            case "axway" -> new AxwayAuthorizationServerClient(
                    settings,
                    buildAxwayCache(cacheDir, asName, environment),
                    buildAxwayTimeout(config, asName),
                    buildAxwayLogger(config, cacheDir, asName, environment)
            );
            default -> throw new IllegalArgumentException("Authorization Server nao suportado: " + asName);
        };
    }

    private AxwayCacheStore buildAxwayCache(Path cacheDir, String asName, String environment) {
        Path file = cacheDir.resolve("axway-cache-" + asName + "-" + environment + ".json");
        return new AxwayCacheStore(file, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private Duration buildAxwayTimeout(AppConfig config, String asName) {
        long seconds = config.getInt("as." + asName + ".timeoutSeconds", 30);
        return Duration.ofSeconds(seconds);
    }

    private AxwayRequestLogger buildAxwayLogger(AppConfig config, Path cacheDir, String asName, String environment) {
        String fileName = config.get("as." + asName + ".logFile", "axway-requests-" + asName + "-" + environment + ".log");
        Path file = cacheDir.resolve(fileName);
        return new AxwayRequestLogger(file);
    }
}
