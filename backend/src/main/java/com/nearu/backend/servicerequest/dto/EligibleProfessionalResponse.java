package com.nearu.backend.servicerequest.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Prestataire pouvant recevoir une demande pour une catégorie donnée. */
public record EligibleProfessionalResponse(
        UUID id,
        String displayName,
        String governorateName,
        BigDecimal avgRating,
        int reviewCount,
        boolean verified) {
}
