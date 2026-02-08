package com.company.scopehandler.axway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class OAuthAppScopeDto {
    @JsonProperty("id")
    private String id;

    @JsonProperty("applicationId")
    private String applicationId;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("isDefault")
    private boolean isDefault;

    public OAuthAppScopeDto() {
    }

    public OAuthAppScopeDto(String applicationId, String scope, boolean isDefault) {
        this.applicationId = applicationId;
        this.scope = scope;
        this.isDefault = isDefault;
    }

    public String getId() {
        return id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getScope() {
        return scope;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
