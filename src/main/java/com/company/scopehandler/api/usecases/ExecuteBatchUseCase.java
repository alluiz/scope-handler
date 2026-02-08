package com.company.scopehandler.api.usecases;

import com.company.scopehandler.api.services.AuditService;
import com.company.scopehandler.api.services.BatchExecutorService;
import com.company.scopehandler.api.services.BatchPlan;
import com.company.scopehandler.api.services.BatchPlannerService;
import com.company.scopehandler.api.services.BatchReport;
import com.company.scopehandler.api.strategy.ModeStrategy;

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
                               com.company.scopehandler.api.cache.ExecutionCache cache) {
        BatchPlan plan = plannerService.plan(clients, scopes);
        return executorService.execute(plan, strategy, auditService, threshold, maxThreads, debug, cache);
    }
}
