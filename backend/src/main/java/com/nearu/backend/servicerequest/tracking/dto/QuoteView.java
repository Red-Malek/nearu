package com.nearu.backend.servicerequest.tracking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.nearu.backend.servicerequest.tracking.QuoteStatus;

/**
 * Devis reçu, tel que le client le consulte (US09).
 *
 * @param canDecide vrai seulement si le client peut encore l'accepter ou le refuser
 */
public record QuoteView(
        UUID id,
        BigDecimal amount,
        String description,
        Instant proposedStart,
        int estimatedDurationMinutes,
        Instant validUntil,
        QuoteStatus status,
        String statusLabel,
        boolean expired,
        boolean canDecide,
        Instant decidedAt,
        Instant createdAt) {
}
