package com.company.scopehandler.providers.axway.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AxwayScopeCacheData {
    @JsonProperty("appScopes")
    private Map<String, AxwayScopeCacheEntry> appScopes = new HashMap<>();

    public Map<String, AxwayScopeCacheEntry> getAppScopes() {
        return appScopes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class AxwayScopeCacheEntry {
        @JsonProperty("timestamp")
        private long timestamp;

        @JsonProperty("scopes")
        private List<String> scopes;

        public AxwayScopeCacheEntry() {
        }

        public AxwayScopeCacheEntry(long timestamp, List<String> scopes) {
            this.timestamp = timestamp;
            this.scopes = scopes;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public List<String> getScopes() {
            return scopes;
        }
    }
}
