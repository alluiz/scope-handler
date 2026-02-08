package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.ports.AuthorizationServerClient;

public final class AssociateStrategy implements ModeStrategy {
    private final AuthorizationServerClient client;
    private final boolean createScope;

    public AssociateStrategy(AuthorizationServerClient client, boolean createScope) {
        this.client = client;
        this.createScope = createScope;
    }

    @Override
    public OperationResult execute(Operation operation) {
        long startedAt = System.currentTimeMillis();
        OperationOutcome createOutcome = null;
        if (createScope) {
            createOutcome = client.createScope(operation.getScope());
        }
        OperationOutcome associateOutcome = client.associateScope(operation.getClientId(), operation.getScope());
        long duration = System.currentTimeMillis() - startedAt;

        String message = buildMessage(createOutcome, associateOutcome);
        return new OperationResult(
                null,
                0,
                0,
                Mode.ASSOCIATE,
                operation.getClientId(),
                operation.getScope(),
                associateOutcome.getStatus(),
                message,
                startedAt,
                duration,
                Thread.currentThread().getName()
        );
    }

    @Override
    public Mode getMode() {
        return Mode.ASSOCIATE;
    }

    private String buildMessage(OperationOutcome createOutcome, OperationOutcome associateOutcome) {
        StringBuilder sb = new StringBuilder();
        if (createOutcome != null) {
            sb.append("createScope[")
                    .append(createOutcome.isSuccess() ? "ok" : "fail")
                    .append("] ")
                    .append(createOutcome.getMessage())
                    .append("; ");
        }
        sb.append("associate[")
                .append(associateOutcome.isSuccess() ? "ok" : "fail")
                .append("] ")
                .append(associateOutcome.getMessage());
        return sb.toString();
    }
}
