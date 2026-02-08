package com.company.scopehandler.providers.axway;

import com.company.scopehandler.api.config.AuthorizationServerSettings;
import com.company.scopehandler.cli.utils.HttpRequestLogger;
import com.company.scopehandler.providers.axway.dto.ApplicationDto;
import com.company.scopehandler.providers.axway.dto.OAuthAppScopeDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public final class AxwayAuthorizationServerClient {
    private static final String BASE_PATH = "/api/portal/v1.2";
    private static final String APP_BY_CLIENT = BASE_PATH + "/applications/oauthclient/{clientId}";
    private static final String APP_SCOPES = BASE_PATH + "/applications/{id}/scope";
    private static final String APP_SCOPE_BY_ID = BASE_PATH + "/applications/{id}/scope/{scopeId}";

    private final String baseUrl;
    private final String authHeader;
    private final HttpRequestLogger requestLogger;
    private final WebClient webClient;
    private final Duration requestTimeout;

    public AxwayAuthorizationServerClient(AuthorizationServerSettings settings,
                                          Duration requestTimeout,
                                          HttpRequestLogger requestLogger) {
        this.baseUrl = normalizeBaseUrl(settings.getBaseUrl());
        this.authHeader = basicAuth(settings.getUsername(), settings.getPassword());
        this.requestTimeout = requestTimeout;
        this.requestLogger = requestLogger;
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader("Authorization", authHeader)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public ApplicationDto fetchApplicationByClientId(String clientId, java.util.Map<String, String> context) {
        String path = APP_BY_CLIENT.replace("{clientId}", urlEncode(clientId));
        try {
            return webClient.get()
                    .uri(path)
                    .exchangeToMono(resp -> toDto(resp, ApplicationDto.class, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            requestLogger.logException(context, e);
            throw new IllegalStateException("Failed to fetch application by clientId", e);
        }
    }

    public OAuthAppScopeDto[] listApplicationScopes(String appId, java.util.Map<String, String> context) {
        String path = APP_SCOPES.replace("{id}", urlEncode(appId));
        try {
            return webClient.get()
                    .uri(path)
                    .exchangeToMono(resp -> toDto(resp, OAuthAppScopeDto[].class, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            requestLogger.logException(context, e);
            throw new IllegalStateException("Failed to list application scopes", e);
        }
    }

    public AxwayResponse createApplicationScope(String appId, String scope, java.util.Map<String, String> context) {
        OAuthAppScopeDto payload = new OAuthAppScopeDto(appId, scope, true);
        try {
            return webClient.post()
                    .uri(APP_SCOPES.replace("{id}", urlEncode(appId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(resp -> toPostResponse(resp, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            requestLogger.logException(context, e);
            throw new IllegalStateException("Failed to create application scope", e);
        }
    }

    public AxwayResponse deleteApplicationScope(String appId, String scopeId, java.util.Map<String, String> context) {
        String path = APP_SCOPE_BY_ID
                .replace("{id}", urlEncode(appId))
                .replace("{scopeId}", urlEncode(scopeId));
        try {
            return webClient.delete()
                    .uri(path)
                    .exchangeToMono(resp -> toDefaultResponse(resp, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            requestLogger.logException(context, e);
            throw new IllegalStateException("Failed to delete application scope", e);
        }
    }

    private <T> Mono<T> toDto(ClientResponse response, Class<T> dtoClass, java.util.Map<String, String> context) {
        requestLogger.logRequest(response.request(), context);
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(dtoClass)
                    .doOnNext(body -> requestLogger.logResponse(response.request(), response, "<dto>", context));
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    requestLogger.logResponse(response.request(), response, body, context);
                    return Mono.error(new AxwayHttpException(response.rawStatusCode()));
                });
    }

    private Mono<AxwayResponse> toDefaultResponse(ClientResponse response, java.util.Map<String, String> context) {
        requestLogger.logRequest(response.request(), context);
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> {
                        requestLogger.logResponse(response.request(), response, body, context);
                        return new AxwayResponse(response.rawStatusCode());
                    });
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    requestLogger.logResponse(response.request(), response, body, context);
                    return Mono.error(new AxwayHttpException(response.rawStatusCode()));
                });
    }

    private Mono<AxwayResponse> toPostResponse(ClientResponse response, java.util.Map<String, String> context) {
        requestLogger.logRequest(response.request(), context);
        if (response.rawStatusCode() == 409) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> {
                        requestLogger.logResponse(response.request(), response, body, context);
                        return new AxwayResponse(409);
                    });
        }
        return toDefaultResponse(response, context);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeBaseUrl(String base) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        String trimmed = base.trim();
        String normalized = trimTrailingSlash(trimmed);
        if (normalized.endsWith(BASE_PATH)) {
            normalized = normalized.substring(0, normalized.length() - BASE_PATH.length());
        }
        return trimTrailingSlash(normalized);
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static final class AxwayResponse {
        private final int statusCode;

        public AxwayResponse(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
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
