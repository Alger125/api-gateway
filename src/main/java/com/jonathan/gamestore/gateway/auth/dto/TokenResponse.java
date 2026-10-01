package com.jonathan.gamestore.gateway.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("token_structure")
    private String tokenStructure;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, String tokenType, long expiresIn, String scope, String tokenStructure) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.scope = scope;
        this.tokenStructure = tokenStructure;
    }

    // Getters y Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getTokenStructure() { return tokenStructure; }
    public void setTokenStructure(String tokenStructure) { this.tokenStructure = tokenStructure; }
}