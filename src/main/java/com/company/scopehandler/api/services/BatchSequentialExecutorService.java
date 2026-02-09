package com.company.scopehandler.api.services;

import com.company.scopehandler.api.cache.ExecutionCache;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.strategy.ModeStrategy;

public final class BatchSequentialExecutorService {
    public void execute(BatchPlan plan,
                        ModeStrategy strategy,
                        BatchResultHandler handler,
                        BatchExecutionLogger logger,
                        ExecutionCache cache,
                        long startNano,
                        BatchExecutionState state) {
        long processed = 0;
        for (Operation operation : plan.getOperations()) {
            OperationResult result = BatchExecutionSupport.maybeSkip(cache, strategy, operation, state);
            handler.handle(result);
            processed++;
            logger.logProgress(processed, plan.getTotalOperations(), startNano, false, state);
        }
        logger.logProgress(processed, plan.getTotalOperations(), startNano, true, state);
    }
}
