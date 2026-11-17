package com.nearu.backend.servicerequest.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.nearu.backend.servicerequest.RequestTiming;
import com.nearu.backend.servicerequest.ServiceRequestStatus;

/**
 * Demande reçue par un prestataire : données prêtes pour la section « Demandes »
 * de son tableau de bord (GET /api/service-requests/received).
 */
public record ReceivedRequestResponse(
        UUID id,
        String customerName,
        String title,
        String description,
        String serviceName,
        RequestTiming timing,
        String timingLabel,
        boolean urgent,
        LocalDate preferredDate,
        LocalTime preferredTime,
        String governorateName,
        String addressLine,
        String addressNote,
        Double latitude,
        Double longitude,
        ServiceRequestStatus status,
        String statusLabel,
        Instant createdAt) {
}
