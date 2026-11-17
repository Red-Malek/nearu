package com.nearu.backend.servicerequest.tracking;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nearu.backend.servicerequest.tracking.dto.MyRequestDetail;
import com.nearu.backend.servicerequest.tracking.dto.MyRequestSummary;

/**
 * « Mes demandes » du client connecté (rôle CUSTOMER, voir SecurityConfig) :
 * suivi des demandes (US10) et décision sur le devis (US09).
 */
@RestController
@RequestMapping("/api/service-requests/mine")
public class CustomerRequestController {

    private final CustomerRequestService service;

    public CustomerRequestController(CustomerRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<MyRequestSummary> list(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(customerId(jwt));
    }

    @GetMapping("/{requestId}")
    public MyRequestDetail detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return service.getMine(customerId(jwt), requestId);
    }

    @PostMapping("/{requestId}/quotes/{quoteId}/accept")
    public MyRequestDetail acceptQuote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
            @PathVariable UUID quoteId) {
        return service.decide(customerId(jwt), requestId, quoteId, true);
    }

    @PostMapping("/{requestId}/quotes/{quoteId}/reject")
    public MyRequestDetail rejectQuote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
            @PathVariable UUID quoteId) {
        return service.decide(customerId(jwt), requestId, quoteId, false);
    }

    private static UUID customerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
