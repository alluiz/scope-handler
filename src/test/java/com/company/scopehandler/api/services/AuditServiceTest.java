package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.OperationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditServiceTest {

    @Test
    void writesAuditFile(@TempDir Path tempDir) throws Exception {
        OperationResult result = new OperationResult(
                "op-1",
                1,
                1,
                Mode.ASSOCIATE,
                "client1",
                "scope1",
                com.company.scopehandler.api.domain.OperationStatus.OK,
                "ok",
                System.currentTimeMillis(),
                10,
                "test-thread"
        );

        Path auditFile;
        try (AuditService auditService = new AuditService(tempDir)) {
            auditService.record(result);
            auditFile = auditService.getFilePath();
        }

        List<String> lines = Files.readAllLines(auditFile);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("timestamp,operationId"));
        assertTrue(lines.get(1).contains("client1"));
    }
}
