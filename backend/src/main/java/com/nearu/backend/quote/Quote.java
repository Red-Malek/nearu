
package com.nearu.backend.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "proposed_start", nullable = false)
    private Instant proposedStart;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Quote() {
    }
    public static Quote create(
        UUID requestId,
        UUID professionalId,
        CreateQuoteRequest draft
) {
    Quote quote = new Quote();

    quote.requestId = requestId;
    quote.professionalId = professionalId;
    quote.amount = draft.amount();
    quote.proposedStart = draft.proposedStart();
    quote.estimatedDurationMinutes = draft.estimatedDurationMinutes();
    quote.validUntil = draft.validUntil();
    quote.status = QuoteStatus.PENDING;
    quote.version = 0;

    return quote;
}
@PrePersist
void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
void onUpdate() {
    updatedAt = Instant.now();
}
public UUID getId() {
    return id;
}

public UUID getRequestId() {
    return requestId;
}

public UUID getProfessionalId() {
    return professionalId;
}

public BigDecimal getAmount() {
    return amount;
}

public Instant getProposedStart() {
    return proposedStart;
}

public Integer getEstimatedDurationMinutes() {
    return estimatedDurationMinutes;
}

public Instant getValidUntil() {
    return validUntil;
}

public QuoteStatus getStatus() {
    return status;
}

public Integer getVersion() {
    return version;
}

public Instant getCreatedAt() {
    return createdAt;
}

public Instant getUpdatedAt() {
    return updatedAt;
}
}
