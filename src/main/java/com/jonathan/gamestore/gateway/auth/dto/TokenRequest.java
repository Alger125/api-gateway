package com.jonathan.gamestore.gateway.auth.dto;

public class TokenRequest {

    private String grantType;
    private String clientId;
    private String clientSecret;
    private String code;
    private String redirectUri;
    private String scope;

    public TokenRequest() {
    }

    public TokenRequest(String grantType, String clientId, String clientSecret, String code, String redirectUri, String scope) {
        this.grantType = grantType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.code = code;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    public String getGrantType() { return grantType; }
    public void setGrantType(String grantType) { this.grantType = grantType; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}