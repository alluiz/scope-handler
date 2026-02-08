package com.company.scopehandler.providers.axway;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.api.config.AuthorizationServerSettings;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.providers.axway.cache.AxwayCacheStore;

import java.nio.file.Path;
import java.time.Duration;

public final class AxwayClientFactory {
    public AuthorizationServerService build(AppConfig config, String environment, Path cacheDir) {
        String asName = "axway";
        AuthorizationServerSettings settings = AuthorizationServerSettings.from(config, asName, environment);
        AxwayCacheStore cacheStore = buildAxwayCache(cacheDir, asName, environment);
        Duration timeout = buildAxwayTimeout(config, asName);
        AxwayRequestLogger logger = buildAxwayLogger(config, cacheDir, asName, environment);
        AxwayAuthorizationServerClient rpcClient = new AxwayAuthorizationServerClient(
                settings,
                timeout,
                logger
        );
        return new AxwayAuthorizationServerService(rpcClient, cacheStore);
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
