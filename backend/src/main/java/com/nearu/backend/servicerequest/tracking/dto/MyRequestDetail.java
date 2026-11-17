package com.nearu.backend.servicerequest.tracking.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.nearu.backend.servicerequest.RequestTiming;
import com.nearu.backend.servicerequest.ServiceRequestStatus;

/** Détail d'une demande du client, avec tous les devis reçus, du plus récent au plus ancien (US10, critère 2). */
public record MyRequestDetail(
        UUID id,
        String title,
        String description,
        String serviceName,
        RequestTiming timing,
        String timingLabel,
        LocalDate preferredDate,
        LocalTime preferredTime,
        String governorateName,
        String addressLine,
        String addressNote,
        ServiceRequestStatus status,
        String statusLabel,
        UUID professionalId,
        String professionalName,
        Instant createdAt,
        Instant updatedAt,
        List<QuoteView> quotes) {

    public MyRequestDetail withQuotes(List<QuoteView> newQuotes) {
        return new MyRequestDetail(id, title, description, serviceName, timing, timingLabel, preferredDate,
                preferredTime, governorateName, addressLine, addressNote, status, statusLabel, professionalId,
                professionalName, createdAt, updatedAt, List.copyOf(newQuotes));
    }
}
