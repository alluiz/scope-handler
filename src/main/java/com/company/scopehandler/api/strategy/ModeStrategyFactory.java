package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.ports.AuthorizationServerService;

public final class ModeStrategyFactory {
    public ModeStrategy create(Mode mode, AuthorizationServerService client, boolean createScope) {
        return switch (mode) {
            case ADD -> new AssociateStrategy(client, createScope);
            case REMOVE -> new DissociateStrategy(client);
            case READ -> new ReadStrategy();
        };
    }
}
