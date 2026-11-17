package com.nearu.backend.auth;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.user.User;
import com.nearu.backend.user.UserRepository;

/**
 * La connexion elle-même (email + mot de passe) et le mot de passe oublié sont
 * gérés par Supabase Auth. Ce controller expose ce que le backend sait de
 * l'utilisateur connecté : son profil et son rôle.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    private final ProfileRegistrationService profileRegistrationService;

    public AuthController(UserRepository userRepository, ProfileRegistrationService profileRegistrationService) {
        this.userRepository = userRepository;
        this.profileRegistrationService = profileRegistrationService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = SupabaseJwtAuthenticationConverter.parseUuid(jwt.getSubject())
            .orElseThrow(() -> ApiException.forbidden("Jeton d'authentification invalide."));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound(
                    "Votre compte n'est pas encore finalisé. Terminez votre inscription pour continuer."));
        if (!user.isActive()) {
            throw ApiException.forbidden("Votre compte est suspendu ou désactivé. Contactez le support.");
        }
        return CurrentUserResponse.from(user);
    }

    /**
     * Finalise l'inscription : crée le profil (rôle, nom, téléphone) à partir des
     * informations saisies dans le formulaire « Créer un compte ». Idempotent.
     */
    @PostMapping("/profile")
    public CurrentUserResponse createProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = profileRegistrationService.getOrCreate(jwt);
        if (!user.isActive()) {
            throw ApiException.forbidden("Votre compte est suspendu ou désactivé. Contactez le support.");
        }
        return CurrentUserResponse.from(user);
    }
}
