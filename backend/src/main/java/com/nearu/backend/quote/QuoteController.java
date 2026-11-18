package com.nearu.backend.quote;

import java.net.URI;
import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/service-requests")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping("/{requestId}/quotes")
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateQuoteRequest body) {

        UUID professionalId = UUID.fromString(jwt.getSubject());

        Quote created = quoteService.create(
            professionalId,
            requestId,
            body
        );

        return ResponseEntity
            .created(URI.create("/api/quotes/" + created.getId()))
            .build();
    }
    @GetMapping("/quotes")
public ResponseEntity<List<QuoteResponse>> getMyQuotes(
        @AuthenticationPrincipal Jwt jwt) {

    UUID professionalId = UUID.fromString(jwt.getSubject());

    return ResponseEntity.ok(
        quoteService.getMyQuotes(professionalId)
    );
}

}