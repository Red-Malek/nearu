package com.nearu.backend.settings;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Paramètres du client connecté. SecurityConfig réserve /api/customer/** au rôle CUSTOMER. */
@RestController
@RequestMapping("/api/customer/settings")
public class CustomerSettingsController {

    private final CustomerSettingsService service;
    private final CurrentUser currentUser;

    public CustomerSettingsController(CustomerSettingsService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/profile")
    public CustomerProfile getProfile(@AuthenticationPrincipal Jwt jwt) {
        return service.getProfile(currentUser.id(), jwt.getClaimAsString("email"));
    }

    @PutMapping("/profile")
    public CustomerProfile updateProfile(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CustomerProfileUpdate body) {
        return service.updateProfile(currentUser.id(), jwt.getClaimAsString("email"), body);
    }

    @GetMapping("/location")
    public CustomerLocation getLocation() {
        return service.getLocation(currentUser.id());
    }

    @PutMapping("/location")
    public CustomerLocation updateLocation(@Valid @RequestBody CustomerLocationUpdate body) {
        return service.updateLocation(currentUser.id(), body);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete() {
        service.delete(currentUser.id());
    }
}
