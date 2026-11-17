package com.nearu.backend.servicerequest.tracking;

/** Statuts autorisés par la contrainte CHECK de quotes.status (V1), avec leur libellé pour le client. */
public enum QuoteStatus {
    PENDING("En attente de votre réponse"),
    ACCEPTED("Devis accepté"),
    REJECTED("Devis refusé"),
    WITHDRAWN("Retiré par le prestataire"),
    EXPIRED("Expiré"),
    CANCELLED("Annulé");

    private final String label;

    QuoteStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
