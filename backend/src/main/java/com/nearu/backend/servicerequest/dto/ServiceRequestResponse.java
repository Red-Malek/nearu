package com.nearu.backend.servicerequest.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.nearu.backend.servicerequest.ServiceRequest;
import com.nearu.backend.servicerequest.ServiceRequestStatus;

/** Demande renvoyée au client après sa création. */
public record ServiceRequestResponse(
        UUID id,
        String title,
        String description,
        String addressLine,
        LocalDate preferredDate,
        ServiceRequestStatus status,
        String statusLabel,
        UUID categoryId,
        String categoryName,
        String otherCategory,
        UUID governorateId,
        UUID professionalId,
        String professionalName,
        Instant createdAt) {

    public static ServiceRequestResponse from(ServiceRequest request, String categoryName, String professionalName) {
        return new ServiceRequestResponse(request.getId(), request.getTitle(), request.getDescription(),
                request.getAddressLine(), request.getPreferredDate(), request.getStatus(),
                request.getStatus().getLabel(), request.getCategoryId(), categoryName, request.getCategoryOther(),
                request.getGovernorateId(),
                request.getProfessionalId(), professionalName, request.getCreatedAt());
    }
}
