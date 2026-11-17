package com.nearu.backend.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.user.User;
import com.nearu.backend.user.UserRepository;
import com.nearu.backend.user.UserRole;

/**
 * Crée le profil applicatif (table users) d'un compte Supabase (US01).
 * <p>
 * Les informations saisies à l'inscription (prénom, nom, téléphone, rôle) sont enregistrées
 * par Supabase dans le claim {@code user_metadata} du JWT. On les lit depuis le jeton déjà
 * validé : cela fonctionne que la confirmation d'e-mail soit activée ou non.
 */
@Service
public class ProfileRegistrationService {

    /** Numéro tunisien normalisé : +216 suivi de 8 chiffres. */
    private static final Pattern TUNISIAN_PHONE = Pattern.compile("^\\+216[0-9]{8}$");

    private static final int NAME_MAX_LENGTH = 60;

    private final UserRepository userRepository;

    public ProfileRegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Renvoie le profil existant, ou le crée à partir des métadonnées d'inscription.
     * L'opération est idempotente : l'appeler deux fois ne crée pas de doublon.
     */
    @Transactional
    public User getOrCreate(Jwt jwt) {
        UUID userId = SupabaseJwtAuthenticationConverter.parseUuid(jwt.getSubject())
            .orElseThrow(() -> ApiException.forbidden("Jeton d'authentification invalide."));

        Optional<User> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Map<String, Object> metadata = jwt.hasClaim("user_metadata") ? jwt.getClaimAsMap("user_metadata") : Map.of();
        String firstName = requiredName(metadata, "first_name", "Le prénom");
        String lastName = requiredName(metadata, "last_name", "Le nom");
        String phone = normalizePhone(text(metadata, "phone"));
        UserRole role = parseRole(text(metadata, "role"));

        if (userRepository.existsByPhone(phone)) {
            throw ApiException.conflict("Ce numéro de téléphone est déjà utilisé par un autre compte.");
        }
        try {
            return userRepository.saveAndFlush(User.register(userId, phone, firstName + " " + lastName, role));
        }
        catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Ce compte ou ce numéro de téléphone existe déjà.");
        }
    }

    static String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[\\s.-]", "");
        if (digits.matches("^[0-9]{8}$")) {
            digits = "+216" + digits;
        }
        else if (digits.startsWith("00216")) {
            digits = "+" + digits.substring(2);
        }
        if (!TUNISIAN_PHONE.matcher(digits).matches()) {
            throw ApiException.badRequest("Le numéro de téléphone doit contenir 8 chiffres (ex. 20 123 456).");
        }
        return digits;
    }

    private static UserRole parseRole(String raw) {
        if ("CUSTOMER".equals(raw)) {
            return UserRole.CUSTOMER;
        }
        if ("PROFESSIONAL".equals(raw)) {
            return UserRole.PROFESSIONAL;
        }
        // Le rôle ADMIN ne peut jamais être choisi à l'inscription.
        throw ApiException.badRequest("Type de compte invalide : choisissez Particulier ou Commerçant / Prestataire.");
    }

    private static String requiredName(Map<String, Object> metadata, String key, String label) {
        String value = text(metadata, key);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(label + " est obligatoire.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw ApiException.badRequest(label + " ne doit pas dépasser " + NAME_MAX_LENGTH + " caractères.");
        }
        return trimmed;
    }

    private static String text(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }
}
