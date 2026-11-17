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

/**
 * Supabase proves who the user is (valid JWT); our own users table says what the user may do.
 * The role is read from the database, never from the token, because token metadata is user-editable.
 * A valid token without an ACTIVE account gets no role.
 */
@Component
public class UserRoleJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserAccountRepository users;

    public UserRoleJwtConverter(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = userId(jwt)
                .flatMap(users::findActive)
                .<GrantedAuthority>map(me -> new SimpleGrantedAuthority("ROLE_" + me.role()))
                .stream()
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private static Optional<UUID> userId(Jwt jwt) {
        try {
            return Optional.of(UUID.fromString(jwt.getSubject()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
