package com.company.scopehandler.ports;

import com.company.scopehandler.domain.OperationOutcome;

public interface AuthorizationServerClient {
    OperationOutcome associateScope(String clientId, String scope);

    OperationOutcome dissociateScope(String clientId, String scope);

    OperationOutcome createScope(String scope);
}
