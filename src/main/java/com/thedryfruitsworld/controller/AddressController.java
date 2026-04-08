package com.thedryfruitsworld.controller;

import com.thedryfruitsworld.entity.Address;
import com.thedryfruitsworld.entity.Profile;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.exception.ResourceNotFoundException;
import com.thedryfruitsworld.repository.AddressRepository;
import com.thedryfruitsworld.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRepository addressRepository;
    private final ProfileRepository profileRepository;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal String userId) {
        List<Address> addresses = addressRepository.findByUserId(UUID.fromString(userId));
        return ResponseEntity.ok(addresses.stream().map(AddressResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@AuthenticationPrincipal String userId,
                                                          @RequestBody Map<String, String> body) {
        Profile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        String line1 = body.get("line1");
        if (line1 == null || line1.isBlank()) {
            throw new BadRequestException("Address line 1 is required");
        }

        // If this is the first address, make it default
        boolean isFirst = addressRepository.findByUserId(profile.getId()).isEmpty();

        Address address = Address.builder()
                .user(profile)
                .label(body.getOrDefault("label", "Home"))
                .line1(line1)
                .line2(body.get("line2"))
                .city(body.getOrDefault("city", ""))
                .state(body.getOrDefault("state", ""))
                .pincode(body.getOrDefault("pincode", ""))
                .isDefault(isFirst)
                .build();

        return ResponseEntity.ok(AddressResponse.from(addressRepository.save(address)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id,
                                               @AuthenticationPrincipal String userId) {
        Address address = addressRepository.findByIdAndUserId(id, UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    @Transactional
    public ResponseEntity<AddressResponse> setDefault(@PathVariable UUID id,
                                                       @AuthenticationPrincipal String userId) {
        UUID uid = UUID.fromString(userId);
        Address target = addressRepository.findByIdAndUserId(id, uid)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Clear existing default
        addressRepository.findByUserId(uid).forEach(a -> {
            if (a.isDefault()) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        });

        target.setDefault(true);
        return ResponseEntity.ok(AddressResponse.from(addressRepository.save(target)));
    }

    // Flat DTO to avoid serializing the lazy Profile association
    public record AddressResponse(
            String id,
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String pincode,
            boolean isDefault
    ) {
        static AddressResponse from(Address a) {
            return new AddressResponse(
                    a.getId().toString(),
                    a.getLabel(),
                    a.getLine1(),
                    a.getLine2(),
                    a.getCity(),
                    a.getState(),
                    a.getPincode(),
                    a.isDefault()
            );
        }
    }
}
