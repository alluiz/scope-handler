package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.domain.OperationStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class BatchExecutionState {
    private final AtomicLong sequence = new AtomicLong(0);
    private final ThreadLocal<Long> threadCounter = ThreadLocal.withInitial(() -> 0L);
    private final AtomicReference<String> lastThreadName = new AtomicReference<>("");
    private final AtomicLong lastSequence = new AtomicLong(0);
    private final Map<String, AtomicLong> threadOps = new ConcurrentHashMap<>();
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong skipCount = new AtomicLong(0);

    public long nextSequence() {
        return sequence.incrementAndGet();
    }

    public OperationResult enrich(OperationResult base, long sequence) {
        long threadIndex = threadCounter.get() + 1;
        threadCounter.set(threadIndex);
        String operationId = java.util.UUID.randomUUID().toString();
        String threadName = Thread.currentThread().getName();
        threadOps.computeIfAbsent(threadName, k -> new AtomicLong(0)).incrementAndGet();
        lastThreadName.set(threadName);
        lastSequence.set(sequence);
        return OperationResult.withMeta(base, operationId, sequence, threadIndex);
    }

    public void updateCounters(OperationResult result) {
        if (result.getStatus() == OperationStatus.OK) {
            successCount.incrementAndGet();
        } else if (result.getStatus() == OperationStatus.FAIL) {
            failureCount.incrementAndGet();
        } else {
            skipCount.incrementAndGet();
        }
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }

    public long getSkipCount() {
        return skipCount.get();
    }

    public long getLastSequence() {
        return lastSequence.get();
    }

    public String getLastThreadName() {
        return lastThreadName.get();
    }

    public Map<String, AtomicLong> getThreadOps() {
        return threadOps;
    }
}
