package com.nearu.backend.servicerequest.dto;

import java.util.List;
import java.util.UUID;

/**
 * Prestataire déjà choisi par le client, affiché en haut du formulaire
 * (« Votre demande est pour ») avec la liste de ses services.
 */
public record RequestTargetResponse(
        UUID id,
        String displayName,
        String governorateName,
        boolean acceptingRequests,
        List<ServiceOption> services) {

    public record ServiceOption(UUID id, String name) {
    }
}
