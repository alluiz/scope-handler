package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationOutcome;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.ports.AuthorizationServerService;

public final class DissociateStrategy implements ModeStrategy {
    private final AuthorizationServerService client;

    public DissociateStrategy(AuthorizationServerService client) {
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
