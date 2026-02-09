package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationServerFactoryTest {

    @Test
    void createsMockClientAndExecutesOperation(@TempDir Path tempDir) {
        AuthorizationServerFactory registry = AuthorizationServerFactory.builder()
                .register("mock", TestClient::new)
                .build();
        AuthorizationServerService client = registry.create("mock");

        assertNotNull(client);
        OperationOutcome outcome = client.associateScope("client1", "scope1");
        assertTrue(outcome.isSuccess());
        assertEquals(200, outcome.getStatusCode());
    }

    @Test
    void failsForUnknownAuthorizationServer(@TempDir Path tempDir) {
        AuthorizationServerFactory registry = AuthorizationServerFactory.builder()
                .register("mock", TestClient::new)
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> registry.create("unknown"));
    }

    @Test
    void allowsCustomRegistration(@TempDir Path tempDir) {
        AuthorizationServerFactory registry = AuthorizationServerFactory.builder()
                .register("mock", TestClient::new)
                .register("custom", TestClient::new)
                .build();
        AuthorizationServerService client = registry.create("custom");
        assertNotNull(client);
    }

    private static final class TestClient implements AuthorizationServerService {
        @Override
        public OperationOutcome associateScope(String clientId, String scope) {
            return OperationOutcome.ok(200, "ok");
        }

        @Override
        public OperationOutcome dissociateScope(String clientId, String scope) {
            return OperationOutcome.ok(200, "ok");
        }

        @Override
        public OperationOutcome createScope(String scope) {
            return OperationOutcome.ok(200, "ok");
        }

        @Override
        public java.util.List<String> listScopes(String clientId) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<String> listClients() {
            return java.util.List.of();
        }

        @Override
        public java.util.List<String> findClientsByScopes(java.util.List<String> scopes,
                                                          com.company.scopehandler.api.domain.FindMatchMode matchMode) {
            return java.util.List.of();
        }
    }
}
