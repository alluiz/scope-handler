package com.company.scopehandler.api.config;

public record Credentials(String username, String password) {
    public boolean isComplete() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String username;
        private String password;

        public Builder username(String username) {
            if (username != null && !username.isBlank()) {
                this.username = username;
            }
            return this;
        }

        public Builder password(String password) {
            if (password != null && !password.isBlank()) {
                this.password = password;
            }
            return this;
        }

        public Credentials build() {
            return new Credentials(username, password);
        }
    }
}
