package com.nearu.backend.auth;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AccountService service;

    public MeController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Me register(@AuthenticationPrincipal Jwt jwt, @RequestBody RegisterRequest request) {
        return service.register(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping
    public Me me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(UUID.fromString(jwt.getSubject()));
    }
}
