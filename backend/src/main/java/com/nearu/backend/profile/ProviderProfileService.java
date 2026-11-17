package com.nearu.backend.profile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProviderProfileService {

    private final ProviderProfileRepository repository;

    public ProviderProfileService(ProviderProfileRepository repository) {
        this.repository = repository;
    }

    /** Unknown, malformed or non-ACTIVE id: 404 (US05 criterion 3). */
    @Transactional(readOnly = true)
    public ProviderProfile getProfile(String id) {
        return parse(id)
                .flatMap(repository::findActiveById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestataire introuvable"));
    }

    private static Optional<UUID> parse(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
