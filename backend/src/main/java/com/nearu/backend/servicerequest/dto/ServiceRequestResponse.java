package com.nearu.backend.servicerequest.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.nearu.backend.servicerequest.RequestTiming;
import com.nearu.backend.servicerequest.ServiceRequest;
import com.nearu.backend.servicerequest.ServiceRequestStatus;

/** Demande renvoyée au client après sa création (écran de confirmation). */
public record ServiceRequestResponse(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        String categoryName,
        String otherCategory,
        RequestTiming timing,
        String timingLabel,
        LocalDate preferredDate,
        LocalTime preferredTime,
        UUID governorateId,
        String addressLine,
        String addressNote,
        ServiceRequestStatus status,
        String statusLabel,
        UUID professionalId,
        String professionalName,
        Instant createdAt) {

    public static ServiceRequestResponse from(ServiceRequest request, String categoryName, String professionalName) {
        return new ServiceRequestResponse(request.getId(), request.getTitle(), request.getDescription(),
                request.getCategoryId(), categoryName, request.getCategoryOther(), request.getTiming(),
                request.getTiming().getLabel(), request.getPreferredDate(), request.getPreferredTime(),
                request.getGovernorateId(), request.getAddressLine(), request.getAddressNote(), request.getStatus(),
                request.getStatus().getLabel(), request.getProfessionalId(), professionalName,
                request.getCreatedAt());
    }
}
