package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.Profile;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileRepository profileRepository;

    @GetMapping
    public ResponseEntity<Profile> getProfile(@AuthenticationPrincipal String userId) {
        Profile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<Profile> updateProfile(@AuthenticationPrincipal String userId,
                                                  @RequestBody Map<String, String> body) {
        Profile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        if (body.containsKey("fullName")) {
            profile.setFullName(body.get("fullName"));
        }
        if (body.containsKey("phone")) {
            profile.setPhone(body.get("phone"));
        }

        return ResponseEntity.ok(profileRepository.save(profile));
    }
}
