package com.nearu.backend.servicerequest;

/**
 * Délai souhaité par le client (section « Quand souhaitez-vous son arrivée ? »),
 * stocké dans service_requests.timing (migration V5).
 */
public enum RequestTiming {
    URGENT("Urgent (dans l'heure)"),
    TODAY("Aujourd'hui"),
    WITHIN_48H("Sous 48 h"),
    SCHEDULED("Date choisie"),
    /** Valeur des demandes créées avant V5. */
    FLEXIBLE("Flexible");

    private final String label;

    RequestTiming(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
