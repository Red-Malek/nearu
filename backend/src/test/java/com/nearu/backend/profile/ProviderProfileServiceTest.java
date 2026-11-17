package com.nearu.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ProviderProfileServiceTest {

    private final ProviderProfileRepository repository = mock(ProviderProfileRepository.class);
    private final ProviderProfileService service = new ProviderProfileService(repository);

    @Test
    void returnsTheProfileOfAnActiveProvider() {
        var id = UUID.randomUUID();
        var ahmed = new ProviderProfile(id, "Ahmed Trabelsi", "Installation électrique.", "Ariana", 15,
                List.of("Électricité"));
        when(repository.findActiveById(id)).thenReturn(Optional.of(ahmed));

        assertThat(service.getProfile(id.toString())).isEqualTo(ahmed);
    }

    @Test
    void answers404WhenTheProviderDoesNotExistOrIsNotActive() {
        var id = UUID.randomUUID();
        when(repository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(id.toString()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void answers404WithoutQueryingWhenTheIdIsMalformed() {
        assertThatThrownBy(() -> service.getProfile("abc"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verifyNoInteractions(repository);
    }
}
