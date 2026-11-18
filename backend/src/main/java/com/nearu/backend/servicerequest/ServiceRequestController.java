package com.nearu.backend.servicerequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

import com.nearu.backend.servicerequest.dto.CreateServiceRequestRequest;
import com.nearu.backend.servicerequest.dto.ReceivedRequestResponse;
import com.nearu.backend.servicerequest.dto.RequestTargetResponse;
import com.nearu.backend.servicerequest.dto.ServiceRequestResponse;

import jakarta.validation.Valid;

/**
 * API de l'envoi de demande (US06). Règles d'accès dans SecurityConfig :
 * <ul>
 * <li>GET /target/{id} : public (le visiteur voit le formulaire avant de se connecter) ;</li>
 * <li>POST : client connecté (rôle CUSTOMER) ;</li>
 * <li>GET /received : prestataire connecté (rôle PROFESSIONAL), pour son tableau de bord.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @GetMapping("/target/{professionalId}")
    public RequestTargetResponse target(@PathVariable UUID professionalId) {
        return service.getTarget(professionalId);
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateServiceRequestRequest body) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        ServiceRequestResponse created = service.create(customerId, body);
        return ResponseEntity.created(URI.create("/api/service-requests/" + created.id())).body(created);
    }

    @GetMapping("/received")
    public List<ReceivedRequestResponse> received(@AuthenticationPrincipal Jwt jwt) {
        return service.findReceived(UUID.fromString(jwt.getSubject()));
    }
    @PatchMapping("/{requestId}/reject")
public ResponseEntity<Void> reject(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID requestId) {

    UUID professionalId = UUID.fromString(jwt.getSubject());
    service.reject(professionalId, requestId);

    return ResponseEntity.noContent().build();
}
}
