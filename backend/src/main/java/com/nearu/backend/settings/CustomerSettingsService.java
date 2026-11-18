package com.nearu.backend.settings;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nearu.backend.common.error.ApiException;

@Service
public class CustomerSettingsService {

    static final CustomerLocation DEFAULT_LOCATION = new CustomerLocation(null, null, null, 3, true, true);

    private final CustomerSettingsRepository repository;

    public CustomerSettingsService(CustomerSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CustomerProfile getProfile(UUID userId, String email) {
        return toProfile(ensureCustomer(userId), email);
    }

    @Transactional
    public CustomerProfile updateProfile(UUID userId, String email, CustomerProfileUpdate in) {
        ensureCustomer(userId);
        String phone = blankToNull(in.phone());
        if (phone != null && repository.phoneUsedByAnother(userId, phone)) {
            throw ApiException.conflict("Ce numéro de téléphone est déjà utilisé.");
        }
        String fullName = (in.firstName().trim() + " " + in.lastName().trim()).trim();
        repository.updateProfile(userId, fullName, phone);
        return toProfile(repository.findUser(userId).orElseThrow(), email);
    }

    @Transactional
    public CustomerLocation getLocation(UUID userId) {
        ensureCustomer(userId);
        return repository.findLocation(userId).orElse(DEFAULT_LOCATION);
    }

    @Transactional
    public CustomerLocation updateLocation(UUID userId, CustomerLocationUpdate in) {
        ensureCustomer(userId);
        repository.saveLocation(userId, blankToNull(in.address()), blankToNull(in.postalCode()),
                blankToNull(in.city()), in.searchRadiusKm(), in.useGps(), in.hideExactAddress());
        return repository.findLocation(userId).orElseThrow();
    }

    @Transactional
    public void delete(UUID userId) {
        ensureCustomer(userId);
        repository.markDeleted(userId);
    }

    private CustomerSettingsRepository.UserRow ensureCustomer(UUID userId) {
        var user = repository.findUser(userId)
                .orElseThrow(() -> ApiException.forbidden("Compte introuvable."));
        if (!"CUSTOMER".equals(user.role()) || !"ACTIVE".equals(user.status())) {
            throw ApiException.forbidden("Espace réservé aux clients actifs.");
        }
        return user;
    }

    /** La base ne stocke que full_name : le prénom est le premier mot, le nom le reste. */
    private static CustomerProfile toProfile(CustomerSettingsRepository.UserRow user, String email) {
        String full = user.fullName() == null ? "" : user.fullName().trim();
        int space = full.indexOf(' ');
        String first = space < 0 ? full : full.substring(0, space);
        String last = space < 0 ? "" : full.substring(space + 1).trim();
        return new CustomerProfile(first, last, email, user.phone());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
