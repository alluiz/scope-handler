package com.company.scopehandler.api.services;

import com.company.scopehandler.api.cache.ExecutionCache;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.strategy.ModeStrategy;

public final class BatchExecutionSupport {
    private BatchExecutionSupport() {
    }

    public static OperationResult maybeSkip(ExecutionCache cache,
                                            ModeStrategy strategy,
                                            Operation operation,
                                            BatchExecutionState state) {
        if (cache != null && cache.isExecuted(strategy.getMode(), operation.getClientId(), operation.getScope())) {
            OperationResult skipped = OperationResultFactory.skipped(
                    strategy.getMode(),
                    operation.getClientId(),
                    operation.getScope(),
                    "skipped (cached)"
            );
            return state.enrich(skipped, state.nextSequence());
        }
        return safeExecute(strategy, operation, state);
    }

    private static OperationResult safeExecute(ModeStrategy strategy,
                                               Operation operation,
                                               BatchExecutionState state) {
        long opSeq = state.nextSequence();
        try {
            OperationResult base = strategy.execute(operation);
            return state.enrich(base, opSeq);
        } catch (Exception e) {
            OperationResult base = OperationResultFactory.failure(
                    "unexpected error: " + e.getMessage(),
                    strategy.getMode(),
                    operation.getClientId(),
                    operation.getScope()
            );
            return state.enrich(base, opSeq);
        }
    }
}
