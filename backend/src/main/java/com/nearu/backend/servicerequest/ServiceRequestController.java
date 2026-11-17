package com.nearu.backend.servicerequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nearu.backend.servicerequest.dto.CreateServiceRequestRequest;
import com.nearu.backend.servicerequest.dto.EligibleProfessionalResponse;
import com.nearu.backend.servicerequest.dto.ServiceRequestResponse;

import jakarta.validation.Valid;

/**
 * API de la fonctionnalité « Envoyer une demande » (US06).
 * Les catégories et gouvernorats du formulaire viennent de /api/categories et /api/governorates.
 * Règles d'accès dans SecurityConfig : liste publique, création réservée au rôle CUSTOMER.
 */
@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    /** categoryId absent = catégorie « Autre » : tous les prestataires disponibles. */
    @GetMapping("/eligible-professionals")
    public List<EligibleProfessionalResponse> eligibleProfessionals(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID governorateId,
            @RequestParam(required = false) String q) {
        return service.findEligibleProfessionals(categoryId, governorateId, q);
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateServiceRequestRequest body) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        ServiceRequestResponse created = service.create(customerId, body);
        return ResponseEntity.created(URI.create("/api/service-requests/" + created.id())).body(created);
    }
}
