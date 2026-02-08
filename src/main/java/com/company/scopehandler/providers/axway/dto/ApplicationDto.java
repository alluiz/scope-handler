package com.company.scopehandler.providers.axway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ApplicationDto {
    @JsonProperty("id")
    private String id;

    public String getId() {
        return id;
    }
}
