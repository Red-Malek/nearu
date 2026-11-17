package com.nearu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AccountServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final AccountService service = new AccountService(users);
    private final UUID id = UUID.randomUUID();

    @Test
    void createsAnAccountWithTheChosenRole() {
        var me = service.register(id, new RegisterRequest("  Ahmed Trabelsi ", "PROFESSIONAL"));

        assertThat(me).isEqualTo(new Me(id, "Ahmed Trabelsi", "PROFESSIONAL"));
        verify(users).insert(id, "Ahmed Trabelsi", "PROFESSIONAL");
    }

    @Test
    void refusesToSelfAssignAdminOrUnknownRoles() {
        for (var role : new String[] {"ADMIN", "root", "", null}) {
            assertThatThrownBy(() -> service.register(id, new RegisterRequest("Ahmed", role)))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verify(users, never()).insert(any(), any(), any());
    }

    @Test
    void refusesABlankName() {
        assertThatThrownBy(() -> service.register(id, new RegisterRequest("   ", "CUSTOMER")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void refusesToCreateTheSameAccountTwice() {
        when(users.exists(id)).thenReturn(true);

        assertThatThrownBy(() -> service.register(id, new RegisterRequest("Ahmed", "CUSTOMER")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).insert(any(), any(), any());
    }

    @Test
    void meReturnsTheActiveAccount() {
        var ahmed = new Me(id, "Ahmed", "CUSTOMER");
        when(users.findActive(id)).thenReturn(Optional.of(ahmed));

        assertThat(service.me(id)).isEqualTo(ahmed);
    }

    @Test
    void meAnswers404WhenThereIsNoActiveAccount() {
        when(users.findActive(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.me(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
