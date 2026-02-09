package com.company.scopehandler.providers.mock;

import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.providers.mock.MockAuthorizationServerClient;

public final class MockAuthorizationServerService implements AuthorizationServerService {
    private final MockAuthorizationServerClient client;

    public MockAuthorizationServerService(MockAuthorizationServerClient client) {
        this.client = client;
    }

    @Override
    public OperationOutcome associateScope(String clientId, String scope) {
        MockAuthorizationServerClient.MockResponse response = client.associateScopeRpc(clientId, scope);
        return toOutcome("associate", response);
    }

    @Override
    public OperationOutcome dissociateScope(String clientId, String scope) {
        MockAuthorizationServerClient.MockResponse response = client.dissociateScopeRpc(clientId, scope);
        return toOutcome("dissociate", response);
    }

    @Override
    public OperationOutcome createScope(String scope) {
        MockAuthorizationServerClient.MockResponse response = client.createScopeRpc(scope);
        return toOutcome("createScope", response);
    }

    @Override
    public java.util.List<String> listScopes(String clientId) {
        return java.util.List.of("scope-a", "scope-b");
    }

    @Override
    public java.util.List<String> listClients() {
        return java.util.List.of("mock-client-1", "mock-client-2", "mock-client-3");
    }

    private OperationOutcome toOutcome(String op, MockAuthorizationServerClient.MockResponse response) {
        int statusCode = response.statusCode();
        String message = op + " status=" + statusCode + " " + response.message();
        if (statusCode >= 200 && statusCode < 300) {
            return OperationOutcome.ok(statusCode, message);
        }
        return OperationOutcome.fail(statusCode, message);
    }
}
