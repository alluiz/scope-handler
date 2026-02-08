package com.company.scopehandler.api.strategy;

import com.company.scopehandler.api.domain.Mode;
import com.company.scopehandler.api.domain.Operation;
import com.company.scopehandler.api.domain.OperationResult;

public interface ModeStrategy {
    OperationResult execute(Operation operation);

    Mode getMode();
}
