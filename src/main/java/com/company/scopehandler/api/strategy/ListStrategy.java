package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.domain.OperationStatus;
import com.company.scopehandler.api.ports.AuthorizationServerService;

import java.util.List;

public final class ListStrategy implements ModeStrategy {
    private final AuthorizationServerService client;

    public ListStrategy(AuthorizationServerService client) {
        this.client = client;
    }

    @Override
    public OperationResult execute(Operation operation) {
        long startedAt = System.currentTimeMillis();
        try {
            List<String> scopes = client.listScopes(operation.getClientId());
            long duration = System.currentTimeMillis() - startedAt;
            String message = "list[ok] scopes=" + String.join(",", scopes);
            return new OperationResult(
                    null,
                    0,
                    0,
                    Mode.LIST,
                    operation.getClientId(),
                    operation.getScope(),
                    OperationStatus.OK,
                    message,
                    startedAt,
                    duration,
                    Thread.currentThread().getName()
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startedAt;
            String message = "list[fail] " + e.getMessage();
            return new OperationResult(
                    null,
                    0,
                    0,
                    Mode.LIST,
                    operation.getClientId(),
                    operation.getScope(),
                    OperationStatus.FAIL,
                    message,
                    startedAt,
                    duration,
                    Thread.currentThread().getName()
            );
        }
    }

    @Override
    public Mode getMode() {
        return Mode.LIST;
    }
}
