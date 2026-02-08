package com.company.scopehandler.api.services;

import com.company.scopehandler.api.domain.OperationResult;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class BatchReport {
    private final Instant startedAt;
    private Instant finishedAt;
    private long total;
    private long successCount;
    private long failureCount;
    private long skipCount;
    private final List<String> sampleErrors = new ArrayList<>();

    public BatchReport() {
        this.startedAt = Instant.now();
    }

    public void add(OperationResult result) {
        total++;
        if (result.getStatus() == com.company.scopehandler.api.domain.OperationStatus.OK) {
            successCount++;
        } else if (result.getStatus() == com.company.scopehandler.api.domain.OperationStatus.FAIL) {
            failureCount++;
            if (sampleErrors.size() < 10) {
                sampleErrors.add("#" + result.getSequence()
                        + " " + result.getClientId() + "/" + result.getScope()
                        + " (" + result.getOperationId() + "): " + result.getMessage());
            }
        } else {
            skipCount++;
        }
    }

    public void finish() {
        this.finishedAt = Instant.now();
    }

    public long getDurationMs() {
        if (finishedAt == null) {
            return -1;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    public long getDurationSeconds() {
        if (finishedAt == null) {
            return -1;
        }
        return Duration.between(startedAt, finishedAt).getSeconds();
    }

    public double getAverageMsPerOperation() {
        if (total <= 0 || finishedAt == null) {
            return 0;
        }
        return (double) getDurationMs() / (double) total;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public long getTotal() {
        return total;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public long getSkipCount() {
        return skipCount;
    }

    public List<String> getSampleErrors() {
        return sampleErrors;
    }
}
