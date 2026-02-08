package com.company.scopehandler.cli.config;

import com.company.scopehandler.api.config.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialsLoaderTest {

    @Test
    void readsEnvStyleCredentials(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("credentials");
        Files.writeString(file, "# comment\nAS_USERNAME=admin\nAS_PASSWORD=secret\n");

        Credentials creds = CredentialsLoader.load(file);

        assertEquals("admin", creds.username());
        assertEquals("secret", creds.password());
        assertTrue(creds.isComplete());
    }
}
