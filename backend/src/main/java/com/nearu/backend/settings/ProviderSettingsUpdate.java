package com.nearu.backend.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Full replacement of the editable fields. A null or blank {@code name} keeps the current one. */
public record ProviderSettingsUpdate(
        @Size(max = 100) String name,
        @Size(max = 1000) String bio,
        @Size(max = 255) String address,
        UUID governorateId,
        @NotNull @Min(1) @Max(100) Integer serviceRadiusKm,
        @Size(max = 30) String phone,
        @NotNull @Size(max = 30) List<UUID> serviceIds,
        @NotNull Boolean visible) {
}
