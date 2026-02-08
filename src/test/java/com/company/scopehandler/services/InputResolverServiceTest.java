package com.company.scopehandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputResolverServiceTest {

    @Test
    void mergesInlineAndFileValues(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("clients.txt");
        Files.writeString(file, "# comment\nclientA\n\nclientB\nclientA\n");

        InputResolverService service = new InputResolverService();
        List<String> result = service.resolve(List.of("clientC", "  "), file);

        assertEquals(List.of("clientC", "clientA", "clientB"), result);
    }
}
