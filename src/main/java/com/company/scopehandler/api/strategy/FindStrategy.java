package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.FindMatchMode;
import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;
import com.company.scopehandler.api.domain.OperationStatus;
import com.company.scopehandler.api.ports.AuthorizationServerService;

import java.util.List;

public final class FindStrategy implements ModeStrategy {
    private final AuthorizationServerService client;
    private final List<String> scopes;
    private final FindMatchMode matchMode;

    public FindStrategy(AuthorizationServerService client, List<String> scopes, FindMatchMode matchMode) {
        this.client = client;
        this.scopes = scopes == null ? List.of() : List.copyOf(scopes);
        this.matchMode = matchMode;
    }

    @Override
    public OperationResult execute(Operation operation) {
        long startedAt = System.currentTimeMillis();
        try {
            List<String> matches = client.findClientsByScopes(scopes, matchMode);
            long duration = System.currentTimeMillis() - startedAt;
            String message = "find[ok] match=" + matchMode.name().toLowerCase()
                    + " scopes=" + String.join(",", scopes)
                    + " clients=" + String.join(",", matches);
            return new OperationResult(
                    null,
                    0,
                    0,
                    Mode.FIND,
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
            String message = "find[fail] " + e.getMessage();
            return new OperationResult(
                    null,
                    0,
                    0,
                    Mode.FIND,
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
        return Mode.FIND;
    }
}
