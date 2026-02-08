package com.company.scopehandler.providers.axway;

import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.providers.axway.AxwayAuthorizationServerClient;
import com.company.scopehandler.providers.axway.cache.AxwayCacheStore;
import com.company.scopehandler.providers.axway.dto.ApplicationDto;
import com.company.scopehandler.providers.axway.dto.OAuthAppScopeDto;

import java.util.HashMap;
import java.util.Map;

public final class AxwayAuthorizationServerService implements AuthorizationServerService {
    private final AxwayAuthorizationServerClient client;
    private final AxwayCacheStore cacheStore;

    public AxwayAuthorizationServerService(AxwayAuthorizationServerClient client, AxwayCacheStore cacheStore) {
        this.client = client;
        this.cacheStore = cacheStore;
    }

    @Override
    public OperationOutcome associateScope(String clientId, String scope) {
        try {
            String appId = resolveApplicationId(clientId);
            java.util.Map<String, String> ctx = new ContextBuilder()
                    .put("clientId", clientId)
                    .put("appId", appId)
                    .put("scope", scope)
                    .build();
            AxwayAuthorizationServerClient.AxwayResponse response = client.createApplicationScope(appId, scope, ctx);
            if (response.getStatusCode() == 409) {
                return OperationOutcome.skip(409, "scope already associated for application id=" + appId + " scope=" + scope);
            }
            return toOutcome("associate", response.getStatusCode());
        } catch (Exception e) {
            return OperationOutcome.fail(-1, "associate failed: " + e.getMessage());
        }
    }

    @Override
    public OperationOutcome dissociateScope(String clientId, String scope) {
        try {
            String appId = resolveApplicationId(clientId);
            String scopeId = resolveScopeId(appId, scope);
            if (scopeId == null) {
                return OperationOutcome.skip(404, "scope not found for application id=" + appId + " scope=" + scope);
            }
            java.util.Map<String, String> ctx = new ContextBuilder()
                    .put("clientId", clientId)
                    .put("appId", appId)
                    .put("scopeId", scopeId)
                    .put("scope", scope)
                    .build();
            AxwayAuthorizationServerClient.AxwayResponse response = client.deleteApplicationScope(appId, scopeId, ctx);
            return toOutcome("dissociate", response.getStatusCode());
        } catch (Exception e) {
            return OperationOutcome.fail(-1, "dissociate failed: " + e.getMessage());
        }
    }

    @Override
    public OperationOutcome createScope(String scope) {
        return OperationOutcome.ok(200, "createScope not required for axway");
    }

    private String resolveApplicationId(String clientId) {
        String cached = cacheStore != null ? cacheStore.getAppId(clientId) : null;
        if (cached != null) {
            return cached;
        }
        ApplicationDto dto = client.fetchApplicationByClientId(clientId, new ContextBuilder()
                .put("clientId", clientId)
                .build());
        if (dto == null || dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalStateException("application id not found in response");
        }
        if (cacheStore != null) {
            cacheStore.putAppId(clientId, dto.getId());
        }
        return dto.getId();
    }

    private String resolveScopeId(String appId, String scope) {
        OAuthAppScopeDto[] scopes = client.listApplicationScopes(appId, new ContextBuilder()
                .put("appId", appId)
                .build());
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

    private OperationOutcome toOutcome(String op, int statusCode) {
        String message = op + " status=" + statusCode;
        if (statusCode >= 200 && statusCode < 300) {
            return OperationOutcome.ok(statusCode, message);
        }
        return OperationOutcome.fail(statusCode, message);
    }

    // ContextBuilder is now used to avoid varargs noise.
}
