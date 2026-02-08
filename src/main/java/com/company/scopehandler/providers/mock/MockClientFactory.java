package com.company.scopehandler.providers.mock;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.api.config.AuthorizationServerSettings;
import com.company.scopehandler.api.ports.AuthorizationServerService;

public final class MockClientFactory {
    public AuthorizationServerService build(AppConfig config, String environment) {
        AuthorizationServerSettings settings = AuthorizationServerSettings.from(config, "mock", environment);
        MockAuthorizationServerClient rpcClient = new MockAuthorizationServerClient(settings);
        return new MockAuthorizationServerService(rpcClient);
    }
}
