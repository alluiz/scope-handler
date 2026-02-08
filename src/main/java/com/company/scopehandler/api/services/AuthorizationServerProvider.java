package com.company.scopehandler.api.services;

import com.company.scopehandler.api.ports.AuthorizationServerService;

public interface AuthorizationServerProvider {
    AuthorizationServerService create();
}
