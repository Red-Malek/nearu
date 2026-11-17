package com.nearu.backend.settings;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProviderSettingsService {

    private final ProviderSettingsRepository repository;

    public ProviderSettingsService(ProviderSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProviderSettings get(UUID userId) {
        return ensureProfile(userId);
    }

    /** US02: a complete profile (métier + ville) can never become incomplete; the owner is the only editor. */
    @Transactional
    public ProviderSettings update(UUID userId, ProviderSettingsUpdate in) {
        ProviderSettings current = ensureProfile(userId);
        List<UUID> serviceIds = in.serviceIds().stream().distinct().toList();

        if (!serviceIds.isEmpty() && repository.countActiveCategories(serviceIds) != serviceIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service inconnu");
        }
        if (in.governorateId() != null && !repository.governorateExists(in.governorateId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ville inconnue");
        }
        boolean completeAfter = in.governorateId() != null && !serviceIds.isEmpty();
        if (current.complete() && !completeAfter) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Au moins un service et une ville sont obligatoires");
        }

        String name = in.name() == null || in.name().isBlank() ? null : in.name().trim();
        repository.updateProfile(userId, name, in.bio(), in.address(), in.governorateId(),
                in.serviceRadiusKm(), in.visible());
        if (in.phone() != null) {
            repository.updatePhone(userId, in.phone().trim());
        }
        repository.replaceServices(userId, serviceIds);
        return repository.find(userId).orElseThrow();
    }

    @Transactional
    public void delete(UUID userId) {
        ensureProfile(userId);
        repository.markDeleted(userId);
    }

    private ProviderSettings ensureProfile(UUID userId) {
        var user = repository.findUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte introuvable"));
        if (!"PROFESSIONAL".equals(user.role()) || !"ACTIVE".equals(user.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Espace réservé aux prestataires actifs");
        }
        if (!repository.profileExists(userId)) {
            repository.createProfile(userId, user.fullName());
        }
        return repository.find(userId).orElseThrow();
    }
}
