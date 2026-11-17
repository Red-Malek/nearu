package com.nearu.backend.servicerequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.dto.CreateServiceRequestRequest;
import com.nearu.backend.servicerequest.dto.ReceivedRequestResponse;
import com.nearu.backend.servicerequest.dto.RequestTargetResponse;
import com.nearu.backend.servicerequest.dto.ServiceRequestResponse;

/**
 * Règles métier de l'envoi d'une demande (US06) : le client a déjà choisi le prestataire
 * sur son profil, décrit son problème, indique quand et où il souhaite l'intervention.
 */
@Service
public class ServiceRequestService {

    /** Les dates « aujourd'hui » et « sous 48 h » sont calculées à l'heure tunisienne. */
    static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final ServiceRequestRepository serviceRequestRepository;

    private final ServiceRequestLookupRepository lookupRepository;

    private final Clock clock;

    @Autowired
    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
            ServiceRequestLookupRepository lookupRepository) {
        this(serviceRequestRepository, lookupRepository, Clock.system(TUNIS));
    }

    ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
            ServiceRequestLookupRepository lookupRepository, Clock clock) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.lookupRepository = lookupRepository;
        this.clock = clock;
    }

    /** En-tête du formulaire : le prestataire choisi et ses services. */
    @Transactional(readOnly = true)
    public RequestTargetResponse getTarget(UUID professionalId) {
        return lookupRepository.findRequestTarget(professionalId)
            .orElseThrow(() -> ApiException.notFound("Ce prestataire est introuvable ou n'est plus disponible."));
    }

    /** Section « Demandes » du tableau de bord du prestataire connecté. */
    @Transactional(readOnly = true)
    public List<ReceivedRequestResponse> findReceived(UUID professionalId) {
        return lookupRepository.findReceived(professionalId);
    }

    /**
     * Crée la demande au statut SUBMITTED (« En attente »), adressée au seul prestataire choisi.
     *
     * @param customerId identifiant du client connecté (claim "sub" du JWT)
     */
    @Transactional
    public ServiceRequestResponse create(UUID customerId, CreateServiceRequestRequest input) {
        if (customerId.equals(input.professionalId())) {
            throw ApiException.badRequest("Vous ne pouvez pas vous envoyer une demande à vous-même.");
        }

        RequestTargetResponse target = lookupRepository.findRequestTarget(input.professionalId())
            .orElseThrow(() -> ApiException.notFound("Ce prestataire est introuvable ou n'est plus disponible."));
        if (!target.acceptingRequests()) {
            throw ApiException.conflict("Ce prestataire n'accepte pas de nouvelles demandes pour le moment.");
        }

        // Service : un de ceux du prestataire, ou « Autre » avec une précision
        String otherCategory = trimToNull(input.otherCategory());
        UUID categoryId;
        String categoryName;
        if (input.categoryId() == null) {
            if (otherCategory == null || otherCategory.length() < 2) {
                throw ApiException.badRequest("Précisez le service concerné (au moins 2 caractères).");
            }
            categoryId = lookupRepository.findOtherCategoryId()
                .orElseThrow(() -> new IllegalStateException(
                        "Catégorie « autre » absente : la migration V4 n'a pas été appliquée."));
            categoryName = "Autre : " + otherCategory;
        }
        else {
            if (otherCategory != null) {
                throw ApiException.badRequest("Choisissez un service de la liste ou « Autre », pas les deux.");
            }
            categoryId = input.categoryId();
            categoryName = lookupRepository.findOfferedCategoryName(input.professionalId(), categoryId)
                .orElseThrow(() -> ApiException.badRequest(
                        "Ce prestataire ne propose pas le service choisi."));
        }

        if (!lookupRepository.governorateExists(input.governorateId())) {
            throw ApiException.badRequest("Le gouvernorat choisi n'existe pas.");
        }
        if ((input.latitude() == null) != (input.longitude() == null)) {
            throw ApiException.badRequest("Position incomplète : latitude et longitude sont nécessaires.");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate preferredDate;
        LocalTime preferredTime = null;
        switch (input.timing()) {
            case URGENT, TODAY -> preferredDate = today;
            case WITHIN_48H -> preferredDate = today.plusDays(2);
            case SCHEDULED -> {
                if (input.preferredDate() == null) {
                    throw ApiException.badRequest("Choisissez une date.");
                }
                if (input.preferredDate().isBefore(today)) {
                    throw ApiException.badRequest("La date choisie ne peut pas être dans le passé.");
                }
                preferredDate = input.preferredDate();
                preferredTime = input.preferredTime();
            }
            default -> throw ApiException.badRequest("Délai souhaité invalide.");
        }

        ServiceRequest saved = serviceRequestRepository.save(ServiceRequest.submit(new NewServiceRequest(
                customerId, input.professionalId(), categoryId, input.categoryId() == null ? otherCategory : null,
                input.title().trim(), input.description().trim(), input.timing(), preferredDate, preferredTime,
                input.governorateId(), input.addressLine().trim(), trimToNull(input.addressNote()),
                input.latitude(), input.longitude())));

        return ServiceRequestResponse.from(saved, categoryName, target.displayName());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
