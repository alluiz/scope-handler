package com.company.scopehandler.providers.axway;

import com.company.scopehandler.cli.utils.http.HttpWebClientFactory;
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
    private static final String APPS = BASE_PATH + "/applications";
    private static final String APP_SCOPES = BASE_PATH + "/applications/{id}/scope";
    private static final String APP_SCOPE_BY_ID = BASE_PATH + "/applications/{id}/scope/{scopeId}";
    private static final String APP_OAUTH = BASE_PATH + "/applications/{id}/oauth";

    private final WebClient webClient;
    private final Duration requestTimeout;

    public AxwayAuthorizationServerClient(WebClient webClient,
                                          Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        this.webClient = webClient;
    }

    public ApplicationDto fetchApplicationByClientId(String clientId, java.util.Map<String, String> context) {
        String path = APP_BY_CLIENT.replace("{clientId}", urlEncode(clientId));
        try {
            return webClient.get()
                    .uri(path)
                    .attribute(HttpWebClientFactory.CONTEXT_ATTR, context)
                    .exchangeToMono(resp -> toDto(resp, ApplicationDto.class, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch application by clientId", e);
        }
    }

    public OAuthAppScopeDto[] listApplicationScopes(String appId, java.util.Map<String, String> context) {
        String path = APP_SCOPES.replace("{id}", urlEncode(appId));
        try {
            return webClient.get()
                    .uri(path)
                    .attribute(HttpWebClientFactory.CONTEXT_ATTR, context)
                    .exchangeToMono(resp -> toDto(resp, OAuthAppScopeDto[].class, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list application scopes", e);
        }
    }

    public ApplicationDto[] listApplications(java.util.Map<String, String> context) {
        try {
            return webClient.get()
                    .uri(APPS)
                    .attribute(HttpWebClientFactory.CONTEXT_ATTR, context)
                    .exchangeToMono(resp -> toDto(resp, ApplicationDto[].class, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list applications", e);
        }
    }

    public com.company.scopehandler.providers.axway.dto.OAuthClientDto[] listApplicationOAuthClients(String appId,
                                                                                                    java.util.Map<String, String> context) {
        try {
            return webClient.get()
                    .uri(APP_OAUTH.replace("{id}", urlEncode(appId)))
                    .attribute(HttpWebClientFactory.CONTEXT_ATTR, context)
                    .exchangeToMono(resp -> toDto(resp, com.company.scopehandler.providers.axway.dto.OAuthClientDto[].class, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list application oauth clients", e);
        }
    }

    public AxwayResponse createApplicationScope(String appId, String scope, java.util.Map<String, String> context) {
        OAuthAppScopeDto payload = new OAuthAppScopeDto(appId, scope, true);
        try {
            return webClient.post()
                    .uri(APP_SCOPES.replace("{id}", urlEncode(appId)))
                    .attribute(HttpWebClientFactory.CONTEXT_ATTR, context)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchangeToMono(resp -> toPostResponse(resp, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
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
                    .attribute(HttpWebClientFactory.CONTEXT_ATTR, context)
                    .exchangeToMono(resp -> toDefaultResponse(resp, context))
                    .timeout(requestTimeout)
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete application scope", e);
        }
    }

    private <T> Mono<T> toDto(ClientResponse response, Class<T> dtoClass, java.util.Map<String, String> context) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(dtoClass);
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    return Mono.error(new AxwayHttpException(response.rawStatusCode()));
                });
    }

    private Mono<AxwayResponse> toDefaultResponse(ClientResponse response, java.util.Map<String, String> context) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> {
                        return new AxwayResponse(response.rawStatusCode());
                    });
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    return Mono.error(new AxwayHttpException(response.rawStatusCode()));
                });
    }

    private Mono<AxwayResponse> toPostResponse(ClientResponse response, java.util.Map<String, String> context) {
        if (response.rawStatusCode() == 409) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> {
                        return new AxwayResponse(409);
                    });
        }
        return toDefaultResponse(response, context);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
