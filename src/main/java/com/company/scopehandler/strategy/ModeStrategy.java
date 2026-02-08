package com.company.scopehandler.strategy;

import com.company.scopehandler.domain.Mode;
import com.company.scopehandler.domain.Operation;
import com.company.scopehandler.domain.OperationResult;

public interface ModeStrategy {
    OperationResult execute(Operation operation);

    Mode getMode();
}
