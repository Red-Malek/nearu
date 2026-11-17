package com.nearu.backend.servicerequest.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.nearu.backend.servicerequest.RequestTiming;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de POST /api/service-requests, envoyé par l'écran « Envoyer une demande ».
 * Les règles sont revérifiées côté serveur, même si le formulaire Angular les applique déjà.
 */
public record CreateServiceRequestRequest(

        @NotNull(message = "Le prestataire est manquant : ouvrez ce formulaire depuis son profil.")
        UUID professionalId,

        /** Service choisi parmi ceux du prestataire ; null quand le client choisit « Autre ». */
        UUID categoryId,

        /** Service précisé par le client quand il choisit « Autre ». */
        @Size(max = 80, message = "Le service précisé ne doit pas dépasser 80 caractères.")
        String otherCategory,

        @NotBlank(message = "Donnez un titre court à votre demande.")
        @Size(max = 80, message = "Le titre ne doit pas dépasser 80 caractères.")
        String title,

        @NotBlank(message = "Décrivez le problème en quelques mots.")
        @Size(min = 10, max = 1000, message = "La description doit contenir entre 10 et 1000 caractères.")
        String description,

        @NotNull(message = "Indiquez quand vous souhaitez l'arrivée du prestataire.")
        RequestTiming timing,

        /** Obligatoire uniquement pour timing = SCHEDULED. */
        LocalDate preferredDate,

        /** Optionnel, uniquement pour timing = SCHEDULED. */
        LocalTime preferredTime,

        @NotNull(message = "Choisissez votre gouvernorat.")
        UUID governorateId,

        @NotBlank(message = "Indiquez l'adresse de l'intervention.")
        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères.")
        String addressLine,

        @Size(max = 200, message = "Le repère ne doit pas dépasser 200 caractères.")
        String addressNote,

        @DecimalMin(value = "-90", message = "Latitude invalide.")
        @DecimalMax(value = "90", message = "Latitude invalide.")
        Double latitude,

        @DecimalMin(value = "-180", message = "Longitude invalide.")
        @DecimalMax(value = "180", message = "Longitude invalide.")
        Double longitude) {
}
