package com.nearu.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.nearu.backend.common.error.ApiException;

class ProfileRegistrationServiceTest {

    @Test
    void normalizesTunisianPhoneNumbers() {
        assertThat(ProfileRegistrationService.normalizePhone("20 123 456")).isEqualTo("+21620123456");
        assertThat(ProfileRegistrationService.normalizePhone("+216 20-123-456")).isEqualTo("+21620123456");
        assertThat(ProfileRegistrationService.normalizePhone("0021620123456")).isEqualTo("+21620123456");
    }

    @Test
    void rejectsInvalidPhoneNumbers() {
        assertThatThrownBy(() -> ProfileRegistrationService.normalizePhone("12345")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ProfileRegistrationService.normalizePhone(null)).isInstanceOf(ApiException.class);
    }
}
