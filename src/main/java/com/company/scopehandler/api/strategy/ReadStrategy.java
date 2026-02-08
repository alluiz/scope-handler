package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.domain.OperationStatus;

public final class ReadStrategy implements ModeStrategy {
    @Override
    public OperationResult execute(Operation operation) {
        long startedAt = System.currentTimeMillis();
        long duration = System.currentTimeMillis() - startedAt;
        return new OperationResult(
                null,
                0,
                0,
                Mode.READ,
                operation.getClientId(),
                operation.getScope(),
                OperationStatus.OK,
                "read[ok] no-op",
                startedAt,
                duration,
                Thread.currentThread().getName()
        );
    }

    @Override
    public Mode getMode() {
        return Mode.READ;
    }
}
