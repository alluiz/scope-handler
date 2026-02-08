package com.company.scopehandler.providers.axway.cache;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AxwayCacheStore {
    private final Path file;
    private final ObjectMapper mapper;
    private final Map<String, String> clientToAppId;
    public AxwayCacheStore(Path file, ObjectMapper mapper) {
        this.file = file;
        this.mapper = mapper;
        AxwayCacheData data = load(file, mapper);
        this.clientToAppId = new ConcurrentHashMap<>(data.getClientToAppId());
    }

    public String getAppId(String clientId) {
        return clientToAppId.get(clientId);
    }

    public void putAppId(String clientId, String appId) {
        clientToAppId.put(clientId, appId);
        persist();
    }

    private synchronized void persist() {
        try {
            Files.createDirectories(file.getParent());
            AxwayCacheData data = new AxwayCacheData();
            data.getClientToAppId().putAll(clientToAppId);
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.writeString(file, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Axway cache: " + file, e);
        }
    }

    private static AxwayCacheData load(Path file, ObjectMapper mapper) {
        if (file == null || !Files.exists(file)) {
            return new AxwayCacheData();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return new AxwayCacheData();
            }
            return mapper.readValue(json, AxwayCacheData.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Axway cache: " + file, e);
        }
    }
}
