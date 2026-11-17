package com.nearu.backend.profile;

import java.util.List;
import java.util.UUID;

/** Public profile of a provider: name, métiers, zone d'intervention and description (US05 criterion 1). */
public record ProviderProfile(
        UUID id,
        String name,
        String bio,
        String city,
        int serviceRadiusKm,
        List<String> services) {
}
