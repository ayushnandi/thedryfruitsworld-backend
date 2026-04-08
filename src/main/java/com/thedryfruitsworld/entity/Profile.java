package com.thedryfruitsworld.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {

    @Id
    private UUID id; // matches auth.users(id) — not auto-generated

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(nullable = false)
    private String role = "CUSTOMER";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
