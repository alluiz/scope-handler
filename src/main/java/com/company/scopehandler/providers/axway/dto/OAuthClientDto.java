package com.company.scopehandler.providers.axway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class OAuthClientDto {
    @JsonProperty("id")
    private String clientId;

    public String getClientId() {
        return clientId;
    }
}
