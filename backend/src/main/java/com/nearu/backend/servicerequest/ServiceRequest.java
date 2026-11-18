package com.nearu.backend.servicerequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Les clés étrangères restent des UUID pour ne pas dépendre des entités Catégorie /
 * Prestataire / Gouvernorat des autres fonctionnalités. Les colonnes non mappées
 * (rejection_reason, quote_count…) gardent leur valeur par défaut (V1).
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

    /** Catégorie saisie par le client quand il a choisi « Autre » (V4). */
    @Column(name = "category_other")
    private String categoryOther;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestTiming timing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "preferred_time")
    private LocalTime preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time_slot", nullable = false)
    private TimeSlot preferredTimeSlot;

    @Column(name = "governorate_id", nullable = false)
    private UUID governorateId;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    /** Repère pour trouver l'adresse (V5). */
    @Column(name = "address_note")
    private String addressNote;

    /** Position du client (« Utiliser ma position actuelle »), optionnelle. */
    @Column(name = "lat")
    private Double latitude;

    @Column(name = "lng")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus status;
    @Column(name = "quote_count", nullable = false)
private Integer quoteCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServiceRequest() {
        // requis par JPA
    }

    /** Nouvelle demande au statut SUBMITTED (« En attente »). */
    public static ServiceRequest submit(NewServiceRequest draft) {
        ServiceRequest request = new ServiceRequest();
        request.customerId = draft.customerId();
        request.professionalId = draft.professionalId();
        request.categoryId = draft.categoryId();
        request.categoryOther = draft.categoryOther();
        request.title = draft.title();
        request.description = draft.description();
        request.timing = draft.timing();
        request.urgency = draft.timing() == RequestTiming.URGENT ? Urgency.URGENT : Urgency.NORMAL;
        request.preferredDate = draft.preferredDate();
        request.preferredTime = draft.preferredTime();
        request.preferredTimeSlot = TimeSlot.of(draft.preferredTime());
        request.governorateId = draft.governorateId();
        request.addressLine = draft.addressLine();
        request.addressNote = draft.addressNote();
        request.latitude = draft.latitude();
        request.longitude = draft.longitude();
        request.status = ServiceRequestStatus.SUBMITTED;
        return request;
    }
    public void reject() {
    if (this.status != ServiceRequestStatus.SUBMITTED) {
        throw new IllegalStateException("Cette demande ne peut plus être refusée.");
    }

    this.status = ServiceRequestStatus.REJECTED;
}
public void addQuote() {
    if (this.status != ServiceRequestStatus.SUBMITTED
        && this.status != ServiceRequestStatus.QUOTED) {
        throw new IllegalStateException(
            "Cette demande ne peut plus recevoir de devis."
        );
    }

    if (this.quoteCount >= 3) {
        throw new IllegalStateException(
            "Le nombre maximum de devis pour cette demande est atteint."
        );
    }

    this.quoteCount++;
    this.status = ServiceRequestStatus.QUOTED;
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

    public RequestTiming getTiming() {
        return timing;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public LocalTime getPreferredTime() {
        return preferredTime;
    }

    public TimeSlot getPreferredTimeSlot() {
        return preferredTimeSlot;
    }

    public UUID getGovernorateId() {
        return governorateId;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getAddressNote() {
        return addressNote;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
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
    public Integer getQuoteCount() {
    return quoteCount;
}
}
