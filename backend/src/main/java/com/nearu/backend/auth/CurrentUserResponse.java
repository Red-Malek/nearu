package com.nearu.backend.auth;

import java.util.UUID;

import com.nearu.backend.user.User;
import com.nearu.backend.user.UserRole;

/**
 * Profil de l'utilisateur connecté, utilisé par le frontend pour l'orienter
 * vers l'espace de son rôle après la connexion.
 */
public record CurrentUserResponse(UUID id, String fullName, UserRole role) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getFullName(), user.getRole());
    }
}
