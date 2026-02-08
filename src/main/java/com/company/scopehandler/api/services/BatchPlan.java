package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.Operation;

public final class BatchPlan {
    private final Iterable<Operation> operations;
    private final long totalOperations;

    public BatchPlan(Iterable<Operation> operations, long totalOperations) {
        this.operations = operations;
        this.totalOperations = totalOperations;
    }

    public Iterable<Operation> getOperations() {
        return operations;
    }

    public long getTotalOperations() {
        return totalOperations;
    }
}
