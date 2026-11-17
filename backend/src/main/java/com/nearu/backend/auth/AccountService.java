package com.nearu.backend.auth;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private final UserAccountRepository users;

    public AccountService(UserAccountRepository users) {
        this.users = users;
    }

    /** Creates the application account for a user that Supabase has just authenticated. */
    @Transactional
    public Me register(UUID id, RegisterRequest request) {
        var role = parseRole(request == null ? null : request.role());
        var name = validName(request.fullName());
        if (users.exists(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte déjà créé");
        }
        users.insert(id, name, role.name());
        return new Me(id, name, role.name());
    }

    @Transactional(readOnly = true)
    public Me me(UUID id) {
        return users.findActive(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable"));
    }

    private static AccountRole parseRole(String role) {
        try {
            return AccountRole.valueOf(role == null ? "" : role);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide");
        }
    }

    private static String validName(String fullName) {
        var name = fullName == null ? "" : fullName.trim();
        if (name.length() < 2 || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom invalide");
        }
        return name;
    }
}
