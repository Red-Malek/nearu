package com.nearu.backend.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Un téléphone vide ou null garde le numéro actuel (la colonne users.phone est NOT NULL). */
public record CustomerProfileUpdate(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @Size(max = 30) String phone) {
}
