package com.company.scopehandler.cli;

import com.company.scopehandler.api.config.Credentials;
import com.company.scopehandler.cli.config.CredentialsLoader;

import java.nio.file.Path;

public final class CredentialsService {
    public Result resolve(String user, String password, Path credentialsFile) {
        if (isComplete(user, password)) {
            return new Result(user, password);
        }

        Path file = credentialsFile != null ? credentialsFile : Path.of("credentials");
        Credentials fileCreds = CredentialsLoader.load(file);
        Credentials.Builder builder = Credentials.builder()
                .username(fileCreds.username())
                .password(fileCreds.password());

        if (user != null) {
            builder.username(user);
        }
        if (password != null) {
            builder.password(password);
        }

        Credentials creds = builder.build();
        if (!creds.isComplete()) {
            Result prompted = promptForCredentials(user, password);
            builder.username(prompted.username()).password(prompted.password());
            creds = builder.build();
        }

        return new Result(creds.username(), creds.password());
    }

    private boolean isComplete(String user, String password) {
        return user != null && !user.isBlank() && password != null && !password.isBlank();
    }

    private Result promptForCredentials(String user, String password) {
        java.io.Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Nao foi possivel ler credenciais. Informe --user/--password ou arquivo.");
        }
        String resolvedUser = user;
        String resolvedPassword = password;
        if (resolvedUser == null || resolvedUser.isBlank()) {
            resolvedUser = console.readLine("Usuario: ");
        }
        if (resolvedPassword == null || resolvedPassword.isBlank()) {
            char[] secret = console.readPassword("Senha: ");
            if (secret != null) {
                resolvedPassword = new String(secret);
            }
        }
        return new Result(resolvedUser, resolvedPassword);
    }

    public record Result(String username, String password) {
    }
}
