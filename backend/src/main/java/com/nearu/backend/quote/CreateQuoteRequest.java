package com.nearu.backend.quote;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateQuoteRequest(

    @NotNull
    @DecimalMin("0.01")
    BigDecimal amount,

    @NotNull
    Instant proposedStart,

    @NotNull
    @Positive
    Integer estimatedDurationMinutes,

    @NotNull
    Instant validUntil

) {}