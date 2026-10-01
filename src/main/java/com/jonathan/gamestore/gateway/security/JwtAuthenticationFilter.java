package com.jonathan.gamestore.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Filtro Reactivo Global de Seguridad para Spring Cloud Gateway.
 * Intercepta todas las peticiones entrantes sobre el Event Loop de Netty.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    // Rutas públicas que no requieren token Bearer
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/token",
            "/api/auth/authorize",
            "/api/auth/inspect",
            "/api/auth/mtls-info",
            "/actuator"
    );

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Permitir acceso libre a los endpoints publicos
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 2. Extraer cabecera Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Falta cabecera 'Authorization: Bearer <token>'");
        }

        // 3. Extraer y validar el token JWT
        String token = authHeader.substring(7).trim();

        if (!jwtService.validateToken(token)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Token JWT invalido, firma alterada o expirado");
        }

        // 4. Inyectar datos del usuario autenticado en cabeceras para los microservicios internos
        try {
            Claims claims = jwtService.extractClaims(token);
            String subject = claims.getSubject();
            Object roles = claims.get("roles");
            Object clientId = claims.get("client_id");

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Auth-Subject", subject != null ? subject : "anonymous")
                    .header("X-Auth-Roles", roles != null ? String.valueOf(roles) : "[]")
                    .header("X-Auth-Client", clientId != null ? String.valueOf(clientId) : "none")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Error al procesar claims del token");
        }
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /**
     * Construye una respuesta JSON 401 reactiva no bloqueante.
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonError = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Alta prioridad: se ejecuta antes del enrutamiento hacia los microservicios
        return -1;
    }
}