package com.nearu.backend.servicerequest.tracking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.nearu.backend.servicerequest.ServiceRequestStatus;

/** Une ligne de « Mes demandes » (US10) : le statut est toujours fourni en toutes lettres. */
public record MyRequestSummary(
        UUID id,
        String title,
        String serviceName,
        UUID professionalId,
        String professionalName,
        ServiceRequestStatus status,
        String statusLabel,
        BigDecimal quoteAmount,
        Instant createdAt,
        Instant updatedAt) {
}
