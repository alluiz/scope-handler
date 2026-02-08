package com.company.scopehandler.cli;

import com.company.scopehandler.api.strategy.ModeStrategy;

import java.nio.file.Path;
import java.util.List;

public record BatchRunInput(
        List<String> clients,
        List<String> scopes,
        ModeStrategy strategy,
        Path auditDir,
        int threshold,
        int threads,
        boolean debug,
        boolean ignoreCache,
        String asName,
        String environment
) {
}
