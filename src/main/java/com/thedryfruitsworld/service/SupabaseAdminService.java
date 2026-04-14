package com.thedryfruitsworld.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Calls the Supabase Auth Admin API to keep {@code app_metadata.role}
 * in sync with the {@code profiles} table.
 *
 * This is a secondary sync — the backend now reads role from the DB
 * (see UserRoleCacheService), but syncing to Supabase ensures the JWT
 * reflects the correct role after the next login / token refresh,
 * which is useful for external clients or future Supabase Edge Functions.
 */
@Service
@Slf4j
public class SupabaseAdminService {

    private final WebClient webClient;

    public SupabaseAdminService(
            @Value("${app.supabase.project-url}") String projectUrl,
            @Value("${app.supabase.service-role-key}") String serviceRoleKey) {
        this.webClient = WebClient.builder()
                .baseUrl(projectUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Fire-and-forget: updates app_metadata.role in Supabase.
     * Errors are logged but never thrown — the DB update is the primary action.
     */
    public void syncRoleToAppMetadata(String userId, String role) {
        webClient.put()
                .uri("/auth/v1/admin/users/{id}", userId)
                .bodyValue(Map.of("app_metadata", Map.of("role", role)))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Synced role {} to Supabase for user {}", role, userId))
                .doOnError(e -> log.warn("Failed to sync role to Supabase app_metadata for {}: {}", userId, e.getMessage()))
                .subscribe();
    }
}
