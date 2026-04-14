package com.thedryfruitsworld.security;

import com.thedryfruitsworld.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single source of truth for user roles.
 *
 * Reads from the {@code profiles} table instead of Supabase JWT app_metadata,
 * so role changes in the admin panel take effect immediately without requiring
 * users to log out and back in.
 *
 * Results are cached for 5 minutes (Caffeine, configured in application.yml)
 * to avoid a DB hit on every authenticated request.
 */
@Service
@RequiredArgsConstructor
public class UserRoleCacheService {

    private final ProfileRepository profileRepository;

    @Cacheable(value = "userRoles", key = "#userId")
    public String getRoleForUser(String userId) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(p -> p.getRole())
                .orElse("CUSTOMER");
    }

    @CacheEvict(value = "userRoles", key = "#userId")
    public void evictRole(String userId) {
        // Annotation handles eviction — new role is picked up on next request
    }
}
