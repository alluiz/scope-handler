package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.ports.AuthorizationServerClient;

public final class ModeStrategyFactory {
    public ModeStrategy create(Mode mode, AuthorizationServerClient client, boolean createScope) {
        return switch (mode) {
            case ASSOCIATE -> new AssociateStrategy(client, createScope);
            case DISSOCIATE -> new DissociateStrategy(client);
        };
    }
}
