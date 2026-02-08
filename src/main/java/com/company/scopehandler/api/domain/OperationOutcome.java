package com.company.scopehandler.api.domain;

public final class OperationOutcome {
    private final OperationStatus status;
    private final int statusCode;
    private final String message;

    public OperationOutcome(OperationStatus status, int statusCode, String message) {
        this.status = status;
        this.statusCode = statusCode;
        this.message = message;
    }

    public boolean isSuccess() {
        return status == OperationStatus.OK;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public static OperationOutcome ok(int statusCode, String message) {
        return new OperationOutcome(OperationStatus.OK, statusCode, message);
    }

    public static OperationOutcome fail(int statusCode, String message) {
        return new OperationOutcome(OperationStatus.FAIL, statusCode, message);
    }

    public static OperationOutcome skip(int statusCode, String message) {
        return new OperationOutcome(OperationStatus.SKIP, statusCode, message);
    }
}
