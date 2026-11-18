package com.nearu.backend.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.nearu.backend.common.error.ApiException;

class CustomerSettingsServiceTest {

    private final CustomerSettingsRepository repository = mock(CustomerSettingsRepository.class);
    private final CustomerSettingsService service = new CustomerSettingsService(repository);
    private final UUID id = UUID.randomUUID();

    private void givenUser(String role, String status) {
        when(repository.findUser(id)).thenReturn(Optional.of(
                new CustomerSettingsRepository.UserRow(role, status, "Camille Dupont de Lyon", "20123456")));
    }

    @Test
    void profileSplitsTheFullNameAtTheFirstSpace() {
        givenUser("CUSTOMER", "ACTIVE");

        CustomerProfile profile = service.getProfile(id, "camille@example.com");

        assertThat(profile.firstName()).isEqualTo("Camille");
        assertThat(profile.lastName()).isEqualTo("Dupont de Lyon");
        assertThat(profile.email()).isEqualTo("camille@example.com");
        assertThat(profile.phone()).isEqualTo("20123456");
    }

    @Test
    void updateRebuildsTheFullNameAndKeepsThePhoneWhenBlank() {
        givenUser("CUSTOMER", "ACTIVE");

        service.updateProfile(id, "c@x.tn", new CustomerProfileUpdate(" Camille ", " Dupont ", "  "));

        verify(repository).updateProfile(id, "Camille Dupont", null);
    }

    @Test
    void aPhoneUsedByAnotherAccountIsRejected() {
        givenUser("CUSTOMER", "ACTIVE");
        when(repository.phoneUsedByAnother(id, "99111222")).thenReturn(true);

        assertThatThrownBy(() -> service.updateProfile(id, "c@x.tn",
                new CustomerProfileUpdate("Camille", "Dupont", "99111222")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).updateProfile(any(), any(), any());
    }

    @Test
    void aProfessionalCannotUseTheCustomerSettings() {
        givenUser("PROFESSIONAL", "ACTIVE");

        assertThatThrownBy(() -> service.getLocation(id))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void locationFallsBackToDefaultsBeforeTheFirstSave() {
        givenUser("CUSTOMER", "ACTIVE");
        when(repository.findLocation(id)).thenReturn(Optional.empty());

        CustomerLocation location = service.getLocation(id);

        assertThat(location.searchRadiusKm()).isEqualTo(3);
        assertThat(location.useGps()).isTrue();
        assertThat(location.hideExactAddress()).isTrue();
    }

    @Test
    void blankLocationFieldsAreStoredAsNull() {
        givenUser("CUSTOMER", "ACTIVE");
        when(repository.findLocation(id)).thenReturn(Optional.of(CustomerSettingsService.DEFAULT_LOCATION));

        service.updateLocation(id, new CustomerLocationUpdate("", " 8000 ", "Nabeul", 5, false, true));

        verify(repository).saveLocation(id, null, "8000", "Nabeul", 5, false, true);
    }

    @Test
    void deletingTheAccountMarksItDeleted() {
        givenUser("CUSTOMER", "ACTIVE");

        service.delete(id);

        verify(repository).markDeleted(id);
    }

    @Test
    void aDeletedAccountCannotBeUsed() {
        givenUser("CUSTOMER", "DELETED");

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ApiException.class);
        verify(repository, never()).markDeleted(any());
        verify(repository, never()).saveLocation(any(), any(), any(), any(), anyInt(), anyBoolean(), anyBoolean());
    }
}
