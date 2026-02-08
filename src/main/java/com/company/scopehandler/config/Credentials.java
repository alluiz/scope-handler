package com.company.scopehandler.config;

public record Credentials(String username, String password) {
    public boolean isComplete() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }
}
