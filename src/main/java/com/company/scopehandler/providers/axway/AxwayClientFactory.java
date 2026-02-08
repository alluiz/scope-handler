package com.company.scopehandler.providers.axway;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.api.config.AuthorizationServerSettings;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.cli.utils.http.HttpRequestLogger;
import com.company.scopehandler.cli.utils.http.HttpWebClientFactory;
import com.company.scopehandler.providers.axway.cache.AxwayCacheStore;

import java.nio.file.Path;
import java.time.Duration;

public final class AxwayClientFactory {
    public AuthorizationServerService build(AppConfig config, String environment, Path cacheDir, boolean debug) {
        String asName = "axway";
        AuthorizationServerSettings settings = AuthorizationServerSettings.from(config, asName, environment);
        AxwayCacheStore cacheStore = buildAxwayCache(cacheDir, asName, environment);
        Duration timeout = buildAxwayTimeout(config, asName);
        HttpRequestLogger logger = buildAxwayLogger(config, cacheDir, asName, environment, debug);
        String baseUrl = normalizeBaseUrl(settings.getBaseUrl());
        String authHeader = basicAuth(settings.getUsername(), settings.getPassword());
        var baseClient = HttpWebClientFactory.build(logger);
        var webClient = baseClient.mutate()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", authHeader)
                .defaultHeader("Accept", "application/json")
                .build();
        AxwayAuthorizationServerClient rpcClient = new AxwayAuthorizationServerClient(webClient, timeout);
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

    private HttpRequestLogger buildAxwayLogger(AppConfig config, Path cacheDir, String asName, String environment, boolean debug) {
        String fileName = config.get("as." + asName + ".logFile", "axway-requests-" + asName + "-" + environment + ".log");
        Path file = cacheDir.resolve(fileName);
        return new HttpRequestLogger(file, debug);
    }

    private String normalizeBaseUrl(String base) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        String trimmed = base.trim();
        String normalized = trimTrailingSlash(trimmed);
        String basePath = "/api/portal/v1.2";
        if (normalized.endsWith(basePath)) {
            normalized = normalized.substring(0, normalized.length() - basePath.length());
        }
        return trimTrailingSlash(normalized);
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
