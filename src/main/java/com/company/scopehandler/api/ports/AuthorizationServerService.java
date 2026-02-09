package com.company.scopehandler.api.ports;

import com.company.scopehandler.api.domain.OperationOutcome;

public interface AuthorizationServerService {
    OperationOutcome associateScope(String clientId, String scope);

    OperationOutcome dissociateScope(String clientId, String scope);

    OperationOutcome createScope(String scope);

    java.util.List<String> listScopes(String clientId);

    java.util.List<String> listClients();

    java.util.List<String> findClientsByScopes(java.util.List<String> scopes,
                                               com.company.scopehandler.api.domain.FindMatchMode matchMode);
}
