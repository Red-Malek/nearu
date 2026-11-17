package com.nearu.backend.profile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderProfileController {

    private final ProviderProfileService service;

    public ProviderProfileController(ProviderProfileService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ProviderProfile get(@PathVariable String id) {
        return service.getProfile(id);
    }
}
