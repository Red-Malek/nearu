package com.nearu.backend.servicerequest.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de POST /api/service-requests (US06). Les règles sont vérifiées côté serveur,
 * même si le formulaire Angular les vérifie déjà.
 */
public record CreateServiceRequestRequest(

        /** Catégorie choisie dans la liste ; null quand le client choisit « Autre ». */
        UUID categoryId,

        /** Catégorie précisée par le client quand il choisit « Autre ». */
        @Size(max = 80, message = "La catégorie précisée ne doit pas dépasser 80 caractères.")
        String otherCategory,

        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 120, message = "Le titre ne doit pas dépasser 120 caractères.")
        String title,

        @NotBlank(message = "La description est obligatoire.")
        @Size(min = 10, max = 1000, message = "La description doit contenir entre 10 et 1000 caractères.")
        String description,

        @NotNull(message = "Choisissez votre gouvernorat.")
        UUID governorateId,

        @NotBlank(message = "L'adresse est obligatoire.")
        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères.")
        String addressLine,

        @FutureOrPresent(message = "La date souhaitée ne peut pas être dans le passé.")
        LocalDate preferredDate,

        @NotNull(message = "Choisissez le prestataire à qui envoyer votre demande.")
        UUID professionalId) {
}
