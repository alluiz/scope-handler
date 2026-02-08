package com.company.scopehandler.usecases;

import com.company.scopehandler.services.AuditService;
import com.company.scopehandler.services.BatchExecutorService;
import com.company.scopehandler.services.BatchPlan;
import com.company.scopehandler.services.BatchPlannerService;
import com.company.scopehandler.services.BatchReport;
import com.company.scopehandler.strategy.ModeStrategy;

import java.util.List;

public final class ExecuteBatchUseCase {
    private final BatchPlannerService plannerService;
    private final BatchExecutorService executorService;

    public ExecuteBatchUseCase(BatchPlannerService plannerService, BatchExecutorService executorService) {
        this.plannerService = plannerService;
        this.executorService = executorService;
    }

    public BatchReport execute(List<String> clients,
                               List<String> scopes,
                               ModeStrategy strategy,
                               AuditService auditService,
                               int threshold,
                               int maxThreads,
                               boolean debug,
                               com.company.scopehandler.cache.ExecutionCache cache) {
        BatchPlan plan = plannerService.plan(clients, scopes);
        return executorService.execute(plan, strategy, auditService, threshold, maxThreads, debug, cache);
    }
}
