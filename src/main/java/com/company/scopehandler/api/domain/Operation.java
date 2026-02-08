package com.company.scopehandler.api.domain;

import java.util.Objects;

public final class Operation {
    private final String clientId;
    private final String scope;

    public Operation(String clientId, String scope) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }
}
