package com.company.scopehandler.api.services;

import com.company.scopehandler.api.strategy.ModeStrategy;
import com.company.scopehandler.api.cache.ExecutionCache;

public final class BatchExecutorService {
    private final TaskExecutorService taskExecutor = new TaskExecutorService();
    private final BatchSequentialExecutorService sequentialExecutor = new BatchSequentialExecutorService();
    private final BatchParallelExecutorService parallelExecutor = new BatchParallelExecutorService(taskExecutor);

    public BatchReport execute(BatchPlan plan,
                               ModeStrategy strategy,
                               AuditService auditService,
                               int threshold,
                               int maxThreads,
                               boolean debugEnabled,
                               ExecutionCache cache) {
        BatchExecutionState state = new BatchExecutionState();
        BatchExecutionLogger logger = new BatchExecutionLogger(debugEnabled);
        long total = plan.getTotalOperations();
        BatchReport report = new BatchReport();
        BatchResultHandler handler = new BatchResultHandler(state, logger, auditService, report, cache);
        long startNano = System.nanoTime();

        if (total >= threshold) {
            parallelExecutor.execute(plan, strategy, handler, logger, maxThreads, cache, startNano, state);
        } else {
            sequentialExecutor.execute(plan, strategy, handler, logger, cache, startNano, state);
        }

        report.finish();
        logger.logThreadSummary(state);
        logger.logFinalStatus(report);
        return report;
    }

}
