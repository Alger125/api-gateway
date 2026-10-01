package com.jonathan.gamestore.gateway.auth.controller;

import com.jonathan.gamestore.gateway.auth.dto.TokenRequest;
import com.jonathan.gamestore.gateway.auth.dto.TokenResponse;
import com.jonathan.gamestore.gateway.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    // Inyeccion explicita por constructor (cero dependencia de plugins de Lombok)
    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Clientes de prueba registrados para OAuth 2.0 Machine-to-Machine
    private static final Map<String, String> REGISTERED_CLIENTS = Map.of(
            "catalog-service-client", "cat-secret-2026",
            "sales-service-client", "sales-secret-2026",
            "frontend-gamestore", "front-secret-2026"
    );

    /**
     * 1. OAuth 2.0 Token Endpoint:
     * Soporta los flujos "client_credentials" y "authorization_code".
     */
    @PostMapping("/token")
    public ResponseEntity<?> issueToken(@RequestBody TokenRequest request) {
        String grantType = request.getGrantType();

        // -------------------------------------------------------------
        // FLUJO 1: Client Credentials (Machine-to-Machine / Microservicios)
        // -------------------------------------------------------------
        if ("client_credentials".equalsIgnoreCase(grantType)) {
            String clientId = request.getClientId();
            String clientSecret = request.getClientSecret();

            // Proteccion contra NullPointerException (Map.of no permite claves nulas)
            if (clientId == null || clientSecret == null || !clientSecret.equals(REGISTERED_CLIENTS.get(clientId))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid_client", "error_description", "client_id o client_secret invalidos o ausentes"));
            }

            String token = jwtService.generateClientToken(clientId, request.getScope());
            String scope = request.getScope() != null ? request.getScope() : "games:read games:write orders:create";

            return ResponseEntity.ok(new TokenResponse(
                    token,
                    "Bearer",
                    3600,
                    scope,
                    "Header.Payload.Signature (HS512)"
            ));
        }

        // -------------------------------------------------------------
        // FLUJO 2: Authorization Code (Intercambio de codigo por token)
        // -------------------------------------------------------------
        if ("authorization_code".equalsIgnoreCase(grantType)) {
            if (request.getCode() == null || !request.getCode().startsWith("AUTH_CODE_")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "invalid_grant", "error_description", "Codigo de autorizacion invalido o expirado"));
            }

            // Simulamos usuario autenticado que autorizo el consentimiento
            String username = "gamer_pro_2026";
            List<String> roles = List.of("ROLE_USER", "ROLE_BUYER");
            String token = jwtService.generateUserToken(username, roles, request.getScope());
            String scope = request.getScope() != null ? request.getScope() : "read";

            return ResponseEntity.ok(new TokenResponse(
                    token,
                    "Bearer",
                    3600,
                    scope,
                    "Header.Payload.Signature (HS512)"
            ));
        }

        return ResponseEntity.badRequest()
                .body(Map.of("error", "unsupported_grant_type", "supported_grants", List.of("client_credentials", "authorization_code")));
    }

    /**
     * 2. OAuth 2.0 Authorization Endpoint:
     * Simula el consentimiento en el flujo "Authorization Code".
     */
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state) {

        if (!"code".equalsIgnoreCase(responseType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "unsupported_response_type", "message", "Debe ser response_type=code"));
        }

        // Generamos un codigo temporal de autorizacion
        String code = "AUTH_CODE_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("flow", "OAuth 2.0 - Authorization Code");
        response.put("authorization_code", code);
        response.put("client_id", clientId);
        response.put("redirect_uri", redirectUri);
        response.put("state", state);
        response.put("next_step", "El backend del cliente debe enviar un POST /api/auth/token con grant_type=authorization_code y este code para obtener su JWT");

        return ResponseEntity.ok(response);
    }

    /**
     * 3. JWT Inspector:
     * Desglosa didacticamente la estructura del JWT.
     */
    @PostMapping("/inspect")
    public ResponseEntity<?> inspectToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe proporcionar el campo 'token'"));
        }
        return ResponseEntity.ok(jwtService.inspectToken(token));
    }

    /**
     * 4. Mutual TLS (mTLS) - Explicacion del concepto:
     */
    @GetMapping("/mtls-info")
    public ResponseEntity<?> mtlsInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("tema", "1.2.8 Seguridad - Mutual TLS (mTLS)");
        info.put("definicion", "Autenticacion criptografica bidireccional basada en certificados digitales X.509 entre cliente y servidor.");
        info.put("diferencia_con_tls_estandar", Map.of(
                "TLS_Estandar", "Solo el cliente valida el certificado del servidor (ej. un navegador en HTTPS).",
                "Mutual_TLS", "Ambos extremos (cliente y servidor) presentan y validan certificados de una Autoridad Certificadora (CA) de confianza mutua."
        ));
        info.put("flujo_handshake", List.of(
                "1. ClientHello (cifrados soportados)",
                "2. ServerHello + Certificado X.509 del Servidor + CertificateRequest",
                "3. Cliente valida certificado del servidor",
                "4. Cliente envia su Certificado X.509 + CertificateVerify (firmado con clave privada)",
                "5. Servidor valida certificado del cliente",
                "6. Canal cifrado simetrico establecido (Zero-Trust)"
        ));
        info.put("aplicacion_en_microservicios", "Se utiliza en redes internas Zero-Trust para garantizar que ningun servicio o atacante intermedio pueda comunicarse sin su certificado valido, comúnmente automatizado por un Service Mesh (Istio/Linkerd).");
        return ResponseEntity.ok(info);
    }
}
