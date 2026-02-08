package com.company.scopehandler.cli;

import com.company.scopehandler.api.config.AppConfig;
import com.company.scopehandler.api.services.AuthorizationServerFactory;
import com.company.scopehandler.api.ports.AuthorizationServerService;
import com.company.scopehandler.providers.axway.AxwayClientFactory;
import com.company.scopehandler.providers.mock.MockClientFactory;

import java.nio.file.Path;

public final class RegistryService {
    private final MockClientFactory mockFactory;
    private final AxwayClientFactory axwayFactory;

    public RegistryService(MockClientFactory mockFactory, AxwayClientFactory axwayFactory) {
        this.mockFactory = mockFactory;
        this.axwayFactory = axwayFactory;
    }

    public AuthorizationServerFactory build(AppConfig config, String environment, Path cacheDir) {
        return AuthorizationServerFactory.builder()
                .register("mock", () -> mockFactory.build(config, environment))
                .register("axway", () -> axwayFactory.build(config, environment, cacheDir))
                .build();
    }
}
