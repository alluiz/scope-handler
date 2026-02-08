package com.company.scopehandler.axway;

import com.company.scopehandler.config.AuthorizationServerSettings;
import com.company.scopehandler.domain.OperationOutcome;
import com.company.scopehandler.ports.AuthorizationServerClient;
import com.company.scopehandler.axway.cache.AxwayCacheStore;
import com.company.scopehandler.axway.dto.ApplicationDto;
import com.company.scopehandler.axway.dto.OAuthAppScopeDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class AxwayAuthorizationServerClient implements AuthorizationServerClient {
    private static final String BASE_PATH = "/api/portal/v1.2";
    private static final String APP_BY_CLIENT = "/applications/oauthclient/{clientId}";
    private static final String APP_SCOPES = "/applications/{id}/scope";
    private static final String APP_SCOPE_BY_ID = "/applications/{id}/scope/{scopeId}";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String authHeader;
    private final AxwayCacheStore cacheStore;

    public AxwayAuthorizationServerClient(AuthorizationServerSettings settings, AxwayCacheStore cacheStore) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.baseUrl = normalizeBaseUrl(settings.getBaseUrl());
        this.authHeader = basicAuth(settings.getUsername(), settings.getPassword());
        this.cacheStore = cacheStore;
    }

    @Override
    public OperationOutcome associateScope(String clientId, String scope) {
        try {
            String appId = getApplicationId(clientId);
            String existingScopeId = getScopeId(appId, scope);
            if (existingScopeId != null) {
                return OperationOutcome.skip(200, "scope already associated for application id=" + appId + " scope=" + scope);
            }
            OAuthAppScopeDto payload = new OAuthAppScopeDto(appId, scope, false);
            String body = mapper.writeValueAsString(payload);
            HttpResponse<String> response = sendRequest("POST", APP_SCOPES.replace("{id}", urlEncode(appId)), body);
            return toOutcome("associate", response);
        } catch (Exception e) {
            return OperationOutcome.fail(-1, "associate failed: " + e.getMessage());
        }
    }

    @Override
    public OperationOutcome dissociateScope(String clientId, String scope) {
        try {
            String appId = getApplicationId(clientId);
            String scopeId = getScopeId(appId, scope);
            if (scopeId == null) {
                return OperationOutcome.skip(404, "scope not found for application id=" + appId + " scope=" + scope);
            }
            String path = APP_SCOPE_BY_ID
                    .replace("{id}", urlEncode(appId))
                    .replace("{scopeId}", urlEncode(scopeId));
            HttpResponse<String> response = sendRequest("DELETE", path, null);
            return toOutcome("dissociate", response);
        } catch (Exception e) {
            return OperationOutcome.fail(-1, "dissociate failed: " + e.getMessage());
        }
    }

    @Override
    public OperationOutcome createScope(String scope) {
        return OperationOutcome.ok(200, "createScope not required for axway");
    }

    private String getApplicationId(String clientId) throws Exception {
        String cached = cacheStore != null ? cacheStore.getAppId(clientId) : null;
        if (cached != null) {
            return cached;
        }
        String path = APP_BY_CLIENT.replace("{clientId}", urlEncode(clientId));
        HttpResponse<String> response = sendRequest("GET", path, null);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        ApplicationDto dto = mapper.readValue(response.body(), ApplicationDto.class);
        if (dto == null || dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalStateException("application id not found in response");
        }
        if (cacheStore != null) {
            cacheStore.putAppId(clientId, dto.getId());
        }
        return dto.getId();
    }

    private String getScopeId(String appId, String scope) throws Exception {
        String path = APP_SCOPES.replace("{id}", urlEncode(appId));
        HttpResponse<String> response = sendRequest("GET", path, null);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        OAuthAppScopeDto[] scopes = mapper.readValue(response.body(), OAuthAppScopeDto[].class);
        Map<String, String> map = new HashMap<>();
        if (scopes != null) {
            for (OAuthAppScopeDto item : scopes) {
                if (item.getScope() != null && item.getId() != null) {
                    map.put(item.getScope(), item.getId());
                }
            }
        }
        return map.get(scope);
    }

    private HttpResponse<String> sendRequest(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(joinUrl(baseUrl, path)))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", authHeader)
                .header("Accept", "application/json");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        builder.method(method, publisher);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private OperationOutcome toOutcome(String op, HttpResponse<String> response) {
        int status = response.statusCode();
        boolean success = status >= 200 && status < 300;
        String message = op + " status=" + status;
        if (response.body() != null && !response.body().isBlank()) {
            String body = response.body().trim();
            if (body.length() > 200) {
                body = body.substring(0, 200) + "...";
            }
            message = message + " body=" + body;
        }
        return success ? OperationOutcome.ok(status, message) : OperationOutcome.fail(status, message);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String joinUrl(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private String normalizeBaseUrl(String base) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        String trimmed = base.trim();
        if (trimmed.endsWith(BASE_PATH)) {
            return trimmed;
        }
        return trimmed + BASE_PATH;
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
