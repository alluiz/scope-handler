package com.company.scopehandler.axway;

import com.company.scopehandler.config.AuthorizationServerSettings;
import com.company.scopehandler.domain.OperationOutcome;
import com.company.scopehandler.ports.AuthorizationServerClient;
import com.company.scopehandler.axway.cache.AxwayCacheStore;
import com.company.scopehandler.axway.dto.ApplicationDto;
import com.company.scopehandler.axway.dto.OAuthAppScopeDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public final class AxwayAuthorizationServerClient implements AuthorizationServerClient {
    private static final String BASE_PATH = "/api/portal/v1.2";
    private static final String APP_BY_CLIENT = "/applications/oauthclient/{clientId}";
    private static final String APP_SCOPES = "/applications/{id}/scope";
    private static final String APP_SCOPE_BY_ID = "/applications/{id}/scope/{scopeId}";

    private final String baseUrl;
    private final String authHeader;
    private final AxwayCacheStore cacheStore;
    private final AxwayRequestLogger requestLogger;
    private final WebClient webClient;
    private final Duration requestTimeout;
    private final Retry retrySpec;

    public AxwayAuthorizationServerClient(AuthorizationServerSettings settings,
                                          AxwayCacheStore cacheStore,
                                          Duration requestTimeout,
                                          Retry retrySpec,
                                          AxwayRequestLogger requestLogger) {
        this.baseUrl = normalizeBaseUrl(settings.getBaseUrl());
        this.authHeader = basicAuth(settings.getUsername(), settings.getPassword());
        this.cacheStore = cacheStore;
        this.requestTimeout = requestTimeout;
        this.retrySpec = retrySpec;
        this.requestLogger = requestLogger;
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("Authorization", authHeader)
                .defaultHeader("Accept", "application/json")
                .filter((request, next) -> {
                    requestLogger.logRequest(request);
                    return next.exchange(request);
                })
                .build();
    }

    @Override
    public OperationOutcome associateScope(String clientId, String scope) {
        try {
            String appId = getApplicationId(clientId);
            OAuthAppScopeDto payload = new OAuthAppScopeDto(appId, scope, true);
            AxwayResponse response = webClient.post()
                    .uri(APP_SCOPES.replace("{id}", urlEncode(appId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(this::toPostResponse)
                    .timeout(requestTimeout)
                    .retryWhen(retrySpec)
                    .block();
            if (response.statusCode == 409) {
                return OperationOutcome.skip(409, "scope already associated for application id=" + appId + " scope=" + scope);
            }
            return response.toOutcome("associate");
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
            AxwayResponse response = webClient.delete()
                    .uri(path)
                    .exchangeToMono(this::toDefaultResponse)
                    .timeout(requestTimeout)
                    .retryWhen(retrySpec)
                    .block();
            return response.toOutcome("dissociate");
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
        ApplicationDto dto = webClient.get()
                .uri(path)
                .exchangeToMono(resp -> toDto(resp, ApplicationDto.class))
                .timeout(requestTimeout)
                .retryWhen(retrySpec)
                .block();
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
        OAuthAppScopeDto[] scopes = webClient.get()
                .uri(path)
                .exchangeToMono(resp -> toDto(resp, OAuthAppScopeDto[].class))
                .timeout(requestTimeout)
                .retryWhen(retrySpec)
                .block();
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

    private <T> Mono<T> toDto(ClientResponse response, Class<T> dtoClass) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(dtoClass)
                    .doOnNext(body -> requestLogger.logResponse(response.request(), response, "<dto>"));
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    requestLogger.logResponse(response.request(), response, body);
                    return Mono.error(new AxwayHttpException(response.rawStatusCode()));
                });
    }

    private Mono<AxwayResponse> toDefaultResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> {
                        requestLogger.logResponse(response.request(), response, body);
                        return new AxwayResponse(response.rawStatusCode());
                    });
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    requestLogger.logResponse(response.request(), response, body);
                    return Mono.error(new AxwayHttpException(response.rawStatusCode()));
                });
    }

    private Mono<AxwayResponse> toPostResponse(ClientResponse response) {
        if (response.rawStatusCode() == 409) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> {
                        requestLogger.logResponse(response.request(), response, body);
                        return new AxwayResponse(409);
                    });
        }
        return toDefaultResponse(response);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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

    private final class AxwayResponse {
        private final int statusCode;

        private AxwayResponse(int statusCode) {
            this.statusCode = statusCode;
        }

        private OperationOutcome toOutcome(String op) {
            String message = op + " status=" + statusCode;
            if (statusCode >= 200 && statusCode < 300) {
                return OperationOutcome.ok(statusCode, message);
            }
            return OperationOutcome.fail(statusCode, message);
        }
    }

    public static final class AxwayHttpException extends RuntimeException {
        private final int statusCode;

        private AxwayHttpException(int statusCode) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
