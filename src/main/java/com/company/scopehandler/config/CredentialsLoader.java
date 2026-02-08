package com.company.scopehandler.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CredentialsLoader {
    private CredentialsLoader() {
    }

    public static Credentials load(Path file) {
        if (file == null || !Files.exists(file)) {
            return new Credentials(null, null);
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Map<String, String> values = new HashMap<>();
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0 || idx == trimmed.length() - 1) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim().toUpperCase();
                String value = trimmed.substring(idx + 1).trim();
                values.put(key, value);
            }
            String username = values.get("AS_USERNAME");
            String password = values.get("AS_PASSWORD");
            return new Credentials(username, password);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read credentials file: " + file, e);
        }
    }
}
