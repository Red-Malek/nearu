package com.nearu.backend.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuoteResponse(
    UUID id,
    UUID requestId,
    BigDecimal amount,
    Instant proposedStart,
    Integer estimatedDurationMinutes,
    Instant validUntil,
    QuoteStatus status,
    Integer version,
    Instant createdAt
) {}