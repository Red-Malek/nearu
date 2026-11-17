package com.nearu.backend.settings;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Settings of the connected provider. SecurityConfig restricts /api/professional/** to the PROFESSIONAL role. */
@RestController
@RequestMapping("/api/professional/settings")
public class ProviderSettingsController {

    private final ProviderSettingsService service;
    private final CurrentUser currentUser;

    public ProviderSettingsController(ProviderSettingsService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ProviderSettings get() {
        return service.get(currentUser.id());
    }

    @PutMapping
    public ProviderSettings update(@Valid @RequestBody ProviderSettingsUpdate body) {
        return service.update(currentUser.id(), body);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete() {
        service.delete(currentUser.id());
    }
}
