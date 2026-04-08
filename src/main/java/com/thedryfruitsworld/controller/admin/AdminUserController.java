package com.thedryfruitsworld.controller.admin;

import com.thedryfruitsworld.entity.Profile;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Set<String> VALID_ROLES = Set.of("CUSTOMER", "MANAGER", "ADMIN");

    private final ProfileRepository profileRepository;

    @GetMapping
    public ResponseEntity<Page<Profile>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        PageRequest pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        return ResponseEntity.ok(profileRepository.findAll(pageable));
    }

    /**
     * Update a user's role.
     * Body: {@code { "role": "MANAGER" }}
     */
    @PatchMapping("/{id}/role")
    @Transactional
    public ResponseEntity<Profile> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String role = body.get("role");
        if (role == null || !VALID_ROLES.contains(role.toUpperCase())) {
            throw new BadRequestException("Invalid role. Must be one of: " + VALID_ROLES);
        }
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        profile.setRole(role.toUpperCase());
        return ResponseEntity.ok(profileRepository.save(profile));
    }
}
