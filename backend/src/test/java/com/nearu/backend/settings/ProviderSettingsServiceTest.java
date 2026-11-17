package com.nearu.backend.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ProviderSettingsServiceTest {

    private final ProviderSettingsRepository repository = mock(ProviderSettingsRepository.class);
    private final ProviderSettingsService service = new ProviderSettingsService(repository);
    private final UUID id = UUID.randomUUID();
    private final UUID city = UUID.randomUUID();
    private final UUID metier = UUID.randomUUID();

    private void givenProvider(ProviderSettings settings) {
        when(repository.findUser(id)).thenReturn(Optional.of(
                new ProviderSettingsRepository.UserRow("PROFESSIONAL", "ACTIVE", "Ahmed")));
        when(repository.profileExists(id)).thenReturn(true);
        when(repository.find(id)).thenReturn(Optional.of(settings));
    }

    private ProviderSettingsUpdate update(UUID governorate, List<UUID> services) {
        return new ProviderSettingsUpdate("Ahmed", "bio", "adresse", governorate, 10, "20123456", services, true);
    }

    @Test
    void aCompleteProfileCannotLoseItsLastMetier() {
        givenProvider(new ProviderSettings("Ahmed", null, null, city, 10, "1", List.of(metier), true, true));

        assertThatThrownBy(() -> service.update(id, update(city, List.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).updateProfile(any(), any(), any(), any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void anIncompleteProfileCanBeSavedAsDraft() {
        givenProvider(new ProviderSettings("Ahmed", null, null, null, 10, "1", List.of(), true, false));

        service.update(id, update(null, List.of()));

        verify(repository).updateProfile(id, "Ahmed", "bio", "adresse", null, 10, true);
    }

    @Test
    void aCustomerCannotUseTheProviderSettings() {
        when(repository.findUser(id)).thenReturn(Optional.of(
                new ProviderSettingsRepository.UserRow("CUSTOMER", "ACTIVE", "Sana")));

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
