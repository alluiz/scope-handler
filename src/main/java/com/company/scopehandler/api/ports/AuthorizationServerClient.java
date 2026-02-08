package com.company.scopehandler.api.ports;

import com.company.scopehandler.api.domain.OperationOutcome;

public interface AuthorizationServerClient {
    OperationOutcome associateScope(String clientId, String scope);

    OperationOutcome dissociateScope(String clientId, String scope);

    OperationOutcome createScope(String scope);
}
