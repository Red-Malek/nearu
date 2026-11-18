package com.nearu.backend.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.nearu.backend.user.User;
import com.nearu.backend.user.UserRepository;

/**
 * Transforme un JWT Supabase déjà validé en authentification Spring Security.
 * <p>
 * Le rôle n'est pas lu dans le jeton (Supabase y met toujours "authenticated") mais
 * dans notre table users : ROLE_CUSTOMER, ROLE_PROFESSIONAL ou ROLE_ADMIN.
 * Un compte inconnu, suspendu ou supprimé reste authentifié mais sans aucun rôle,
 * il ne peut donc accéder à aucun endpoint protégé par rôle.
 */
@Component
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public SupabaseJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = parseUuid(jwt.getSubject())
                .flatMap(userRepository::findById)
                .filter(User::isActive)
                .map(user -> List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .orElse(List.of());
                
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    static Optional<UUID> parseUuid(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        }
        catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
