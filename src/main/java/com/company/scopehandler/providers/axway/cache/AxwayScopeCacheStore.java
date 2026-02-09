package com.company.scopehandler.providers.axway.cache;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AxwayScopeCacheStore {
    private final Path file;
    private final ObjectMapper mapper;
    private final Map<String, AxwayScopeCacheData.AxwayScopeCacheEntry> appScopes;

    public AxwayScopeCacheStore(Path file, ObjectMapper mapper) {
        this.file = file;
        this.mapper = mapper;
        AxwayScopeCacheData data = load(file, mapper);
        this.appScopes = new ConcurrentHashMap<>(data.getAppScopes());
    }

    public List<String> getScopes(String appId, long ttlMillis) {
        AxwayScopeCacheData.AxwayScopeCacheEntry entry = appScopes.get(appId);
        if (entry == null) {
            return null;
        }
        long age = System.currentTimeMillis() - entry.getTimestamp();
        if (age > ttlMillis) {
            return null;
        }
        List<String> scopes = entry.getScopes();
        return scopes == null ? List.of() : Collections.unmodifiableList(scopes);
    }

    public void putScopes(String appId, List<String> scopes) {
        AxwayScopeCacheData.AxwayScopeCacheEntry entry =
                new AxwayScopeCacheData.AxwayScopeCacheEntry(System.currentTimeMillis(), scopes);
        appScopes.put(appId, entry);
        persist();
    }

    private synchronized void persist() {
        try {
            Files.createDirectories(file.getParent());
            AxwayScopeCacheData data = new AxwayScopeCacheData();
            data.getAppScopes().putAll(appScopes);
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.writeString(file, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Axway scope cache: " + file, e);
        }
    }

    private static AxwayScopeCacheData load(Path file, ObjectMapper mapper) {
        if (file == null || !Files.exists(file)) {
            return new AxwayScopeCacheData();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return new AxwayScopeCacheData();
            }
            return mapper.readValue(json, AxwayScopeCacheData.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Axway scope cache: " + file, e);
        }
    }
}
