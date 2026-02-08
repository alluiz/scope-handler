package com.company.scopehandler.strategy;

import com.company.scopehandler.domain.Mode;
import com.company.scopehandler.domain.Operation;
import com.company.scopehandler.domain.OperationOutcome;
import com.company.scopehandler.domain.OperationResult;
import com.company.scopehandler.ports.AuthorizationServerClient;

public final class DissociateStrategy implements ModeStrategy {
    private final AuthorizationServerClient client;

    public DissociateStrategy(AuthorizationServerClient client) {
        this.client = client;
    }

    @Override
    public OperationResult execute(Operation operation) {
        long startedAt = System.currentTimeMillis();
        OperationOutcome outcome = client.dissociateScope(operation.getClientId(), operation.getScope());
        long duration = System.currentTimeMillis() - startedAt;

        return new OperationResult(
                null,
                0,
                0,
                Mode.DISSOCIATE,
                operation.getClientId(),
                operation.getScope(),
                outcome.getStatus(),
                "dissociate[" + outcome.getStatus().name().toLowerCase() + "] " + outcome.getMessage(),
                startedAt,
                duration,
                Thread.currentThread().getName()
        );
    }

    @Override
    public Mode getMode() {
        return Mode.DISSOCIATE;
    }
}
