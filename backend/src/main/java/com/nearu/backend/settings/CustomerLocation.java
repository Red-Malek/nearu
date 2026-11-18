package com.nearu.backend.settings;

/** Préférences de localisation du client connecté. */
public record CustomerLocation(
        String address,
        String postalCode,
        String city,
        int searchRadiusKm,
        boolean useGps,
        boolean hideExactAddress) {
}
