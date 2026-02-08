package com.company.scopehandler.providers.axway.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AxwayCacheData {
    @JsonProperty("clientToAppId")
    private Map<String, String> clientToAppId = new HashMap<>();

    public Map<String, String> getClientToAppId() {
        return clientToAppId;
    }
}
