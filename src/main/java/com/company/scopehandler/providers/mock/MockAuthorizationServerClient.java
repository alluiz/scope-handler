package com.company.scopehandler.providers.mock;

import com.company.scopehandler.api.config.AuthorizationServerSettings;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MockAuthorizationServerClient {
    private static final int DEFAULT_LATENCY_MS = 100;
    private static final int ASSOCIATE_LATENCY_MS = 2000;
    private static final String ASSOCIATE_ENDPOINT = "/clients/{clientId}/scopes/{scope}";
    private static final String DISSOCIATE_ENDPOINT = "/clients/{clientId}/scopes/{scope}";
    private static final String CREATE_SCOPE_ENDPOINT = "/scopes";
    private final AuthorizationServerSettings settings;

    public MockAuthorizationServerClient(AuthorizationServerSettings settings) {
        this.settings = settings;
    }

    public MockResponse associateScopeRpc(String clientId, String scope) {
        String url = buildUrl(ASSOCIATE_ENDPOINT, clientId, scope);
        return simulate("ASSOCIATE", url, clientId, scope, ASSOCIATE_LATENCY_MS);
    }

    public MockResponse dissociateScopeRpc(String clientId, String scope) {
        String url = buildUrl(DISSOCIATE_ENDPOINT, clientId, scope);
        return simulate("DISSOCIATE", url, clientId, scope, DEFAULT_LATENCY_MS);
    }

    public MockResponse createScopeRpc(String scope) {
        String url = buildUrl(CREATE_SCOPE_ENDPOINT, null, scope);
        return simulate("CREATE_SCOPE", url, null, scope, DEFAULT_LATENCY_MS);
    }

    private MockResponse simulate(String action, String url, String clientId, String scope, int latencyMs) {
        String context = "[AS=" + settings.getName()
                + " env=" + settings.getEnvironment()
                + "] " + action
                + " url=" + url
                + " client=" + valueOrDash(clientId)
                + " scope=" + valueOrDash(scope)
                + " user=" + settings.getUsername();

        sleep(latencyMs);
        return new MockResponse(200, context + " status=200");
    }

    private void sleep(int latencyMs) {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildUrl(String endpointTemplate, String clientId, String scope) {
        String path = endpointTemplate;
        if (clientId != null) {
            path = path.replace("{clientId}", urlEncode(clientId));
        }
        if (scope != null) {
            path = path.replace("{scope}", urlEncode(scope));
        }
        return joinUrl(settings.getBaseUrl(), path);
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

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    public record MockResponse(int statusCode, String message) {
    }
}
