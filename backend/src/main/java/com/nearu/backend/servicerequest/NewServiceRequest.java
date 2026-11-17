package com.nearu.backend.servicerequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Données validées d'une nouvelle demande, prêtes à être enregistrées. */
public record NewServiceRequest(
        UUID customerId,
        UUID professionalId,
        UUID categoryId,
        String categoryOther,
        String title,
        String description,
        RequestTiming timing,
        LocalDate preferredDate,
        LocalTime preferredTime,
        UUID governorateId,
        String addressLine,
        String addressNote,
        Double latitude,
        Double longitude) {
}
