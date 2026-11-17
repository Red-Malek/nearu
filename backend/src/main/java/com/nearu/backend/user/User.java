package com.nearu.backend.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Profil applicatif d'un utilisateur (table users, migration V1).
 * L'identifiant est celui de l'utilisateur Supabase Auth (claim "sub" du JWT) :
 * l'email et le mot de passe sont gérés par Supabase, pas par cette table.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String phone;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private String locale;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected User() {
        // requis par JPA
    }

    /** Nouveau profil créé à l'inscription, avec l'identifiant de l'utilisateur Supabase. */
    public static User register(UUID id, String phone, String fullName, UserRole role) {
        User user = new User();
        user.id = id;
        user.phone = phone;
        user.fullName = fullName;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        user.locale = "fr";
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getLocale() {
        return locale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
