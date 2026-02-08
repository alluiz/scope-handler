package com.company.scopehandler.strategy;

import com.company.scopehandler.domain.Mode;
import com.company.scopehandler.ports.AuthorizationServerClient;

public final class ModeStrategyFactory {
    public ModeStrategy create(Mode mode, AuthorizationServerClient client, boolean createScope) {
        return switch (mode) {
            case ASSOCIATE -> new AssociateStrategy(client, createScope);
            case DISSOCIATE -> new DissociateStrategy(client);
        };
    }
}
