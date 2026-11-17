package com.nearu.backend.servicerequest;

/**
 * Statuts autorisés par la contrainte CHECK de service_requests.status,
 * avec le libellé affiché à l'utilisateur (backlog : statut écrit en toutes lettres).
 */
public enum ServiceRequestStatus {
    SUBMITTED("En attente"),
    ACCEPTED("Acceptée"),
    REJECTED("Refusée"),
    QUOTED("Devis envoyé"),
    /** Le client a accepté le devis (US09, migration V6). */
    QUOTE_ACCEPTED("Devis accepté"),
    /** Le client a refusé le devis (US09, migration V6). */
    QUOTE_REJECTED("Devis refusé"),
    SCHEDULED("Planifiée"),
    COMPLETED("Terminée"),
    CANCELLED("Annulée"),
    EXPIRED("Expirée");

    private final String label;

    ServiceRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
