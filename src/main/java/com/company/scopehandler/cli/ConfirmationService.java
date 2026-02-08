package com.company.scopehandler.cli;

import java.io.Console;
import java.util.Locale;

public final class ConfirmationService {
    public void confirmDestructiveOperation(boolean confirmedFlag) {
        if (confirmedFlag) {
            return;
        }
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Desassociacao exige confirmacao. Use --confirm.");
        }
        String answer = console.readLine("Digite YES para confirmar desassociacao: ");
        if (answer == null || !answer.trim().toUpperCase(Locale.ROOT).equals("YES")) {
            throw new IllegalStateException("Operacao cancelada pelo usuario.");
        }
    }
}
