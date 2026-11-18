package com.nearu.backend.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Remplacement complet des préférences de localisation. */
public record CustomerLocationUpdate(
        @Size(max = 255) String address,
        @Size(max = 10) String postalCode,
        @Size(max = 100) String city,
        @NotNull @Min(1) @Max(100) Integer searchRadiusKm,
        @NotNull Boolean useGps,
        @NotNull Boolean hideExactAddress) {
}
