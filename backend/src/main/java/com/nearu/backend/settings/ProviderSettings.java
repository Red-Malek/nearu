package com.nearu.backend.settings;

import java.util.List;
import java.util.UUID;

/** Editable profile of the connected provider (US02). {@code complete} = at least one métier and one ville. */
public record ProviderSettings(
        String name,
        String bio,
        String address,
        UUID governorateId,
        int serviceRadiusKm,
        String phone,
        List<UUID> serviceIds,
        boolean visible,
        boolean complete) {
}
