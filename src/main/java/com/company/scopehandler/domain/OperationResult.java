package com.company.scopehandler.domain;

public final class OperationResult {
    private final String operationId;
    private final long sequence;
    private final long threadIndex;
    private final Mode mode;
    private final String clientId;
    private final String scope;
    private final OperationStatus status;
    private final String message;
    private final long startedAtEpochMs;
    private final long durationMs;
    private final String threadName;

    public OperationResult(
            String operationId,
            long sequence,
            long threadIndex,
            Mode mode,
            String clientId,
            String scope,
            OperationStatus status,
            String message,
            long startedAtEpochMs,
            long durationMs,
            String threadName) {
        this.operationId = operationId;
        this.sequence = sequence;
        this.threadIndex = threadIndex;
        this.mode = mode;
        this.clientId = clientId;
        this.scope = scope;
        this.status = status;
        this.message = message;
        this.startedAtEpochMs = startedAtEpochMs;
        this.durationMs = durationMs;
        this.threadName = threadName;
    }

    public static OperationResult withMeta(OperationResult base, String operationId, long sequence, long threadIndex) {
        return new OperationResult(
                operationId,
                sequence,
                threadIndex,
                base.mode,
                base.clientId,
                base.scope,
                base.status,
                base.message,
                base.startedAtEpochMs,
                base.durationMs,
                base.threadName
        );
    }

    public String getOperationId() {
        return operationId;
    }

    public long getSequence() {
        return sequence;
    }

    public long getThreadIndex() {
        return threadIndex;
    }

    public Mode getMode() {
        return mode;
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }

    public boolean isSuccess() {
        return status == OperationStatus.OK;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public long getStartedAtEpochMs() {
        return startedAtEpochMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getThreadName() {
        return threadName;
    }
}
