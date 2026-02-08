package com.company.scopehandler.api.services;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.ports.AuthorizationServerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.Properties;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationServerFactoryTest {

    @Test
    void createsMockClientAndExecutesOperation(@TempDir Path tempDir) {
        AppConfig config = new AppConfig(mockProps());
        AuthorizationServerClient client = new AuthorizationServerFactory().create("mock", "dev", config, tempDir);

        assertNotNull(client);
        OperationOutcome outcome = client.associateScope("client1", "scope1");
        assertTrue(outcome.isSuccess());
        assertEquals(200, outcome.getStatusCode());
    }

    @Test
    void failsForUnknownAuthorizationServer(@TempDir Path tempDir) {
        AppConfig config = new AppConfig(mockProps());
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationServerFactory().create("unknown", "dev", config, tempDir));
    }

    private Properties mockProps() {
        Properties props = new Properties();
        props.setProperty("as.mock.env.dev.baseUrl", "https://mock.dev");
        props.setProperty("as.mock.auth.username", "user");
        props.setProperty("as.mock.auth.password", "pass");
        return props;
    }
}
