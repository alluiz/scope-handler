package com.company.scopehandler.providers.axway;

import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.cli.utils.ContextBuilder;
import com.company.scopehandler.providers.axway.AxwayAuthorizationServerClient;
import com.company.scopehandler.providers.axway.cache.AxwayCacheStore;
import com.company.scopehandler.providers.axway.cache.AxwayScopeCacheStore;
import com.company.scopehandler.providers.axway.dto.ApplicationDto;
import com.company.scopehandler.providers.axway.dto.OAuthAppScopeDto;
import com.company.scopehandler.providers.axway.dto.OAuthClientDto;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AxwayAuthorizationServerService implements AuthorizationServerService {
    private final AxwayAuthorizationServerClient client;
    private final AxwayCacheStore cacheStore;
    private final AxwayScopeCacheStore scopeCacheStore;
    private static final long SCOPE_TTL_MS = 24L * 60L * 60L * 1000L;

    public AxwayAuthorizationServerService(AxwayAuthorizationServerClient client,
                                           AxwayCacheStore cacheStore,
                                           AxwayScopeCacheStore scopeCacheStore) {
        this.client = client;
        this.cacheStore = cacheStore;
        this.scopeCacheStore = scopeCacheStore;
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

    @Override
    public List<String> listScopes(String clientId) {
        String appId = resolveApplicationId(clientId);
        return loadAppScopes(appId);
    }

    @Override
    public List<String> listClients() {
        ApplicationDto[] apps = client.listApplications(new ContextBuilder().build());
        if (apps == null || apps.length == 0) {
            return List.of();
        }
        Set<String> clients = new LinkedHashSet<>();
        for (ApplicationDto app : apps) {
            if (app == null || app.getId() == null || app.getId().isBlank()) {
                continue;
            }
            clients.addAll(listClientsForApp(app.getId()));
        }
        return List.copyOf(clients);
    }

    @Override
    public List<String> findClientsByScopes(List<String> scopes,
                                            com.company.scopehandler.api.domain.FindMatchMode matchMode) {
        ApplicationDto[] apps = client.listApplications(new ContextBuilder().build());
        if (apps == null || apps.length == 0) {
            return List.of();
        }
        Set<String> matches = new LinkedHashSet<>();
        for (ApplicationDto app : apps) {
            if (app == null || app.getId() == null || app.getId().isBlank()) {
                continue;
            }
            String appId = app.getId();
            List<String> appScopes = loadAppScopes(appId);
            if (!matchesScopes(appScopes, scopes, matchMode)) {
                continue;
            }
            matches.addAll(listClientsForApp(appId));
        }
        return List.copyOf(matches);
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

    private List<String> loadAppScopes(String appId) {
        List<String> cached = scopeCacheStore != null ? scopeCacheStore.getScopes(appId, SCOPE_TTL_MS) : null;
        if (cached != null) {
            return cached;
        }
        OAuthAppScopeDto[] scopes = client.listApplicationScopes(appId, new ContextBuilder()
                .put("appId", appId)
                .build());
        List<String> values = new java.util.ArrayList<>();
        if (scopes != null) {
            for (OAuthAppScopeDto item : scopes) {
                if (item != null && item.getScope() != null && !item.getScope().isBlank()) {
                    values.add(item.getScope());
                }
            }
        }
        if (scopeCacheStore != null) {
            scopeCacheStore.putScopes(appId, values);
        }
        return values;
    }

    private List<String> listClientsForApp(String appId) {
        OAuthClientDto[] oauthClients = client.listApplicationOAuthClients(appId, new ContextBuilder()
                .put("appId", appId)
                .build());
        if (oauthClients == null || oauthClients.length == 0) {
            return List.of();
        }
        List<String> clients = new java.util.ArrayList<>();
        for (OAuthClientDto oauthClient : oauthClients) {
            if (oauthClient != null && oauthClient.getClientId() != null && !oauthClient.getClientId().isBlank()) {
                clients.add(oauthClient.getClientId());
            }
        }
        return clients;
    }

    private boolean matchesScopes(List<String> appScopes,
                                  List<String> expectedScopes,
                                  com.company.scopehandler.api.domain.FindMatchMode matchMode) {
        if (expectedScopes == null || expectedScopes.isEmpty()) {
            return true;
        }
        if (appScopes == null || appScopes.isEmpty()) {
            return false;
        }
        if (matchMode == com.company.scopehandler.api.domain.FindMatchMode.OR) {
            for (String scope : expectedScopes) {
                if (appScopes.contains(scope)) {
                    return true;
                }
            }
            return false;
        }
        return appScopes.containsAll(expectedScopes);
    }

    // ContextBuilder is now used to avoid varargs noise.
}
