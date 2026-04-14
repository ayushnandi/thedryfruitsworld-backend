package com.thedryfruitsworld.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates Supabase JWTs signed with ES256 (ECDSA P-256).
 *
 * On startup, fetches the project's JWKS endpoint and builds EC public keys
 * indexed by kid. The parser uses a keyLocator so it automatically picks the
 * right key even when Supabase rotates its signing keys.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${app.supabase.project-url}")
    private String supabaseProjectUrl;

    private final Map<String, PublicKey> verificationKeys = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadPublicKeys() {
        try {
            String jwksUrl = supabaseProjectUrl + "/auth/v1/.well-known/jwks.json";
            byte[] jwksBytes = URI.create(jwksUrl).toURL().openStream().readAllBytes();
            JsonNode root = objectMapper.readTree(jwksBytes);
            JsonNode keys = root.get("keys");
            if (keys != null && keys.isArray()) {
                for (JsonNode jwk : keys) {
                    String kty = jwk.path("kty").asText();
                    String alg = jwk.path("alg").asText();
                    String kid = jwk.path("kid").asText();
                    if ("EC".equals(kty) && "ES256".equals(alg)) {
                        String x = jwk.path("x").asText();
                        String y = jwk.path("y").asText();
                        PublicKey key = buildEcPublicKey(x, y);
                        verificationKeys.put(kid, key);
                        log.info("Loaded Supabase ES256 verification key kid={}", kid);
                    }
                }
            }
            if (verificationKeys.isEmpty()) {
                log.error("No EC verification keys loaded from Supabase JWKS — all authenticated requests will be rejected");
            }
        } catch (Exception e) {
            log.error("Failed to load Supabase JWKS from {}: {}", supabaseProjectUrl, e.getMessage());
        }
    }

    public Claims validateSupabaseToken(String token) {
        return Jwts.parser()
                .keyLocator(header -> verificationKeys.get((String) header.get("kid")))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            validateSupabaseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserId(String token) {
        return validateSupabaseToken(token).getSubject();
    }

    public String getRole(String token) {
        Claims claims = validateSupabaseToken(token);
        Object appMetadata = claims.get("app_metadata");
        if (appMetadata instanceof Map<?, ?> map) {
            Object role = map.get("role");
            return role != null ? role.toString() : "CUSTOMER";
        }
        return "CUSTOMER";
    }

    /**
     * Reconstructs an EC P-256 public key from a JWK's Base64URL-encoded x/y coordinates.
     */
    private static PublicKey buildEcPublicKey(String xBase64Url, String yBase64Url) throws Exception {
        byte[] xBytes = Base64.getUrlDecoder().decode(xBase64Url);
        byte[] yBytes = Base64.getUrlDecoder().decode(yBase64Url);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1")); // P-256
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

        ECPublicKeySpec keySpec = new ECPublicKeySpec(new ECPoint(x, y), ecSpec);
        return KeyFactory.getInstance("EC").generatePublic(keySpec);
    }
}
