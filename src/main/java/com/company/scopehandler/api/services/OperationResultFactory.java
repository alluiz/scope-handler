package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.OperationResult;

public final class OperationResultFactory {
    private OperationResultFactory() {
    }

    public static OperationResult failure(String message, Mode mode) {
        return new OperationResult(
                null,
                0,
                0,
                mode,
                "unknown",
                "unknown",
                com.company.scopehandler.api.domain.OperationStatus.FAIL,
                message,
                System.currentTimeMillis(),
                0,
                Thread.currentThread().getName()
        );
    }

    public static OperationResult failure(String message, Mode mode, String clientId, String scope) {
        return new OperationResult(
                null,
                0,
                0,
                mode,
                clientId,
                scope,
                com.company.scopehandler.api.domain.OperationStatus.FAIL,
                message,
                System.currentTimeMillis(),
                0,
                Thread.currentThread().getName()
        );
    }

    public static OperationResult skipped(Mode mode, String clientId, String scope, String message) {
        return new OperationResult(
                null,
                0,
                0,
                mode,
                clientId,
                scope,
                com.company.scopehandler.api.domain.OperationStatus.SKIP,
                message,
                System.currentTimeMillis(),
                0,
                Thread.currentThread().getName()
        );
    }
}
