package com.thedryfruitsworld.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.supabase.jwt-secret}")
    private String jwtSecret;

    public Claims validateSupabaseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
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
        // Supabase stores custom role in app_metadata
        Object appMetadata = claims.get("app_metadata");
        if (appMetadata instanceof java.util.Map<?, ?> map) {
            Object role = map.get("role");
            return role != null ? role.toString() : "CUSTOMER";
        }
        return "CUSTOMER";
    }
}
