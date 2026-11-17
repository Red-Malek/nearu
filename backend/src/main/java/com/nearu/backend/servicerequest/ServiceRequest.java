package com.nearu.backend.servicerequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Demande de service envoyée par un client à un prestataire (table service_requests).
 * <p>
 * Les clés étrangères sont gardées sous forme d'UUID pour ne pas dépendre des entités
 * Catégorie / Prestataire / Gouvernorat développées par les autres membres de l'équipe.
 * Les colonnes non mappées (lat, lng, preferred_time_slot, urgency, quote_count…)
 * prennent leur valeur par défaut définie dans la migration V1.
 */
@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "professional_id", nullable = false, updatable = false)
    private UUID professionalId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    /** Catégorie saisie par le client quand il a choisi « Autre » (migration V4). */
    @Column(name = "category_other")
    private String categoryOther;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "governorate_id", nullable = false)
    private UUID governorateId;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServiceRequest() {
        // requis par JPA
    }

    public static ServiceRequest submit(UUID customerId, UUID professionalId, UUID categoryId,
            String categoryOther, String title, String description, String addressLine, UUID governorateId,
            LocalDate preferredDate) {
        ServiceRequest request = new ServiceRequest();
        request.customerId = customerId;
        request.professionalId = professionalId;
        request.categoryId = categoryId;
        request.categoryOther = categoryOther;
        request.title = title;
        request.description = description;
        request.addressLine = addressLine;
        request.governorateId = governorateId;
        request.preferredDate = preferredDate;
        request.status = ServiceRequestStatus.SUBMITTED;
        return request;
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

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getProfessionalId() {
        return professionalId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryOther() {
        return categoryOther;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public UUID getGovernorateId() {
        return governorateId;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public ServiceRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
