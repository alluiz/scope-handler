package com.company.scopehandler.api.services;

import com.company.scopehandler.api.ports.AuthorizationServerService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AuthorizationServerFactory {
    private final Map<String, AuthorizationServerProvider> providers = new HashMap<>();

    public static Builder builder() {
        return new Builder();
    }

    public AuthorizationServerService create(String asName) {
        AuthorizationServerProvider provider = providers.get(normalize(asName));
        if (provider == null) {
            throw new IllegalArgumentException("Authorization Server nao suportado: " + asName);
        }
        return provider.create();
    }

    private AuthorizationServerFactory register(String asName, AuthorizationServerProvider provider) {
        Objects.requireNonNull(asName, "asName");
        Objects.requireNonNull(provider, "provider");
        providers.put(normalize(asName), provider);
        return this;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public static final class Builder {
        private final AuthorizationServerFactory factory = new AuthorizationServerFactory();

        public Builder register(String asName, AuthorizationServerProvider provider) {
            factory.register(asName, provider);
            return this;
        }

        public AuthorizationServerFactory build() {
            return factory;
        }
    }
}
