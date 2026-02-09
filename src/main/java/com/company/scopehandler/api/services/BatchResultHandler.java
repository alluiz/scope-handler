package com.company.scopehandler.api.services;

import com.company.scopehandler.api.cache.ExecutionCache;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.domain.OperationStatus;

public final class BatchResultHandler {
    private final BatchExecutionState state;
    private final BatchExecutionLogger logger;
    private final AuditService auditService;
    private final BatchReport report;
    private final ExecutionCache cache;

    public BatchResultHandler(BatchExecutionState state,
                              BatchExecutionLogger logger,
                              AuditService auditService,
                              BatchReport report,
                              ExecutionCache cache) {
        this.state = state;
        this.logger = logger;
        this.auditService = auditService;
        this.report = report;
        this.cache = cache;
    }

    public void handle(OperationResult result) {
        state.updateCounters(result);
        logger.logOperationStatus(result);
        auditService.record(result);
        report.add(result);
        if (cache != null && result.getStatus() == OperationStatus.OK) {
            cache.record(result.getMode(), result.getClientId(), result.getScope());
        }
    }

    public BatchExecutionState getState() {
        return state;
    }
}
