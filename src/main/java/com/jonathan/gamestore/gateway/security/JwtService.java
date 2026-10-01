package com.jonathan.gamestore.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generateClientToken(String clientId, String scope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("grant_type", "client_credentials");
        claims.put("client_id", clientId);
        claims.put("scope", scope != null ? scope : "games:read games:write orders:create");
        claims.put("roles", Collections.singletonList("ROLE_SERVICE"));

        return buildToken(clientId, claims);
    }

    public String generateUserToken(String username, List<String> roles, String scope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("grant_type", "authorization_code");
        claims.put("roles", roles != null ? roles : Collections.singletonList("ROLE_USER"));
        claims.put("scope", scope != null ? scope : "read");

        return buildToken(username, claims);
    }

    private String buildToken(String subject, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .header().type("JWT").and()
                .subject(subject)
                .issuer("gamestore-api-gateway")
                .audience().add("gamestore-microservices").and()
                .issuedAt(now)
                .expiration(expiryDate)
                .claims(extraClaims)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public Map<String, Object> inspectToken(String token) {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            var jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            JwsHeader header = jws.getHeader();
            Claims payload = jws.getPayload();

            info.put("valid", true);
            info.put("algorithm", header.getAlgorithm());
            info.put("type", header.getType());
            info.put("subject", payload.getSubject());
            info.put("issuer", payload.getIssuer());
            info.put("issuedAt", payload.getIssuedAt());
            info.put("expiration", payload.getExpiration());
            info.put("claims", payload);
            info.put("structure", "Header (Base64Url) . Payload (Base64Url) . Signature (" + header.getAlgorithm() + ")");
        } catch (JwtException | IllegalArgumentException e) {
            info.put("valid", false);
            info.put("error", e.getMessage());
        }
        return info;
    }
}
