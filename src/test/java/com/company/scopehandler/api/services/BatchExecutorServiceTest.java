package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.strategy.ModeStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchExecutorServiceTest {

    @Test
    void executesAllOperationsInParallel(@TempDir Path tempDir) {
        BatchPlannerService plannerService = new BatchPlannerService();
        BatchPlan plan = plannerService.plan(List.of("c1", "c2"), List.of("s1", "s2"));

        ModeStrategy strategy = new ModeStrategy() {
            @Override
            public OperationResult execute(Operation operation) {
                return new OperationResult(
                        null,
                        0,
                        0,
                        Mode.ASSOCIATE,
                        operation.getClientId(),
                        operation.getScope(),
                        com.company.scopehandler.api.domain.OperationStatus.OK,
                        "ok",
                        System.currentTimeMillis(),
                        1,
                        Thread.currentThread().getName()
                );
            }

            @Override
            public Mode getMode() {
                return Mode.ASSOCIATE;
            }
        };

        BatchExecutorService executorService = new BatchExecutorService();
        BatchReport report;
        try (AuditService auditService = new AuditService(tempDir)) {
            report = executorService.execute(plan, strategy, auditService, 1, 2, false, null);
        }

        assertEquals(4, report.getTotal());
        assertEquals(4, report.getSuccessCount());
        assertEquals(0, report.getFailureCount());
    }
}
