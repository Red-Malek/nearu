package com.nearu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class UserRoleJwtConverterTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final UserRoleJwtConverter converter = new UserRoleJwtConverter(users);

    private static Jwt tokenFor(String subject) {
        return Jwt.withTokenValue("t").header("alg", "ES256").subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    @Test
    void takesTheRoleFromTheDatabase() {
        var id = UUID.randomUUID();
        when(users.findActive(id)).thenReturn(Optional.of(new Me(id, "Ahmed", "PROFESSIONAL")));

        var auth = converter.convert(tokenFor(id.toString()));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PROFESSIONAL");
    }

    @Test
    void givesNoRoleToAValidTokenWithoutAnActiveAccount() {
        var id = UUID.randomUUID();
        when(users.findActive(id)).thenReturn(Optional.empty());

        assertThat(converter.convert(tokenFor(id.toString())).getAuthorities()).isEmpty();
    }

    @Test
    void givesNoRoleWhenTheSubjectIsNotAUuid() {
        assertThat(converter.convert(tokenFor("not-a-uuid")).getAuthorities()).isEmpty();
    }
}
