package com.company.scopehandler.services;

import com.company.scopehandler.config.AppConfig;
import com.company.scopehandler.config.AuthorizationServerSettings;
import com.company.scopehandler.ports.AuthorizationServerClient;
import com.company.scopehandler.axway.AxwayAuthorizationServerClient;
import com.company.scopehandler.axway.cache.AxwayCacheStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public final class AuthorizationServerFactory {
    public AuthorizationServerClient create(String asName, String environment, AppConfig config, Path cacheDir) {
        AuthorizationServerSettings settings = AuthorizationServerSettings.from(config, asName, environment);
        return switch (asName) {
            case "mock" -> new MockAuthorizationServerClient(settings);
            case "axway" -> new AxwayAuthorizationServerClient(settings, buildAxwayCache(cacheDir, asName, environment));
            default -> throw new IllegalArgumentException("Authorization Server nao suportado: " + asName);
        };
    }

    private AxwayCacheStore buildAxwayCache(Path cacheDir, String asName, String environment) {
        Path file = cacheDir.resolve("axway-cache-" + asName + "-" + environment + ".json");
        return new AxwayCacheStore(file, new ObjectMapper());
    }
}
