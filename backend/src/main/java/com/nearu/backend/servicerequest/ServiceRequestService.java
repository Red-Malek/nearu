package com.nearu.backend.servicerequest;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.ServiceRequestLookupRepository.ProfessionalTarget;
import com.nearu.backend.servicerequest.dto.CreateServiceRequestRequest;
import com.nearu.backend.servicerequest.dto.EligibleProfessionalResponse;
import com.nearu.backend.servicerequest.dto.ServiceRequestResponse;

/**
 * Règles métier de l'envoi d'une demande de service (US06).
 */
@Service
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;

    private final ServiceRequestLookupRepository lookupRepository;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
            ServiceRequestLookupRepository lookupRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.lookupRepository = lookupRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibleProfessionalResponse> findEligibleProfessionals(UUID categoryId, UUID governorateId,
            String nameQuery) {
        return lookupRepository.findEligibleProfessionals(categoryId, governorateId, nameQuery);
    }

    /**
     * Crée la demande au statut SUBMITTED (« En attente »).
     * Le client choisit soit une catégorie de la liste, soit « Autre » en précisant sa catégorie.
     *
     * @param customerId identifiant du client connecté (claim "sub" du JWT)
     */
    @Transactional
    public ServiceRequestResponse create(UUID customerId, CreateServiceRequestRequest input) {
        if (customerId.equals(input.professionalId())) {
            throw ApiException.badRequest("Vous ne pouvez pas vous envoyer une demande à vous-même.");
        }

        String otherCategory = input.otherCategory() == null ? "" : input.otherCategory().trim();
        boolean isOther = input.categoryId() == null;
        UUID categoryId;
        String categoryName;
        if (isOther) {
            if (otherCategory.length() < 2) {
                throw ApiException.badRequest("Précisez la catégorie de service souhaitée (au moins 2 caractères).");
            }
            categoryId = lookupRepository.findOtherCategoryId()
                .orElseThrow(() -> new IllegalStateException(
                        "Catégorie « autre » absente : la migration V4 n'a pas été appliquée."));
            categoryName = "Autre : " + otherCategory;
        }
        else {
            if (!otherCategory.isEmpty()) {
                throw ApiException.badRequest("Choisissez une catégorie de la liste ou « Autre », pas les deux.");
            }
            categoryId = input.categoryId();
            categoryName = lookupRepository.findActiveCategoryName(categoryId)
                .orElseThrow(() -> ApiException.badRequest(
                        "La catégorie choisie n'existe pas ou n'est plus disponible."));
        }

        if (!lookupRepository.governorateExists(input.governorateId())) {
            throw ApiException.badRequest("Le gouvernorat choisi n'existe pas.");
        }

        ProfessionalTarget professional = lookupRepository
            .findProfessionalTarget(input.professionalId(), isOther ? null : categoryId)
            .filter(ProfessionalTarget::activeProfessional)
            .orElseThrow(() -> ApiException.notFound("Ce prestataire est introuvable."));

        if (!professional.acceptingRequests()) {
            throw ApiException.conflict("Ce prestataire n'accepte pas de nouvelles demandes pour le moment.");
        }
        if (!professional.offersCategory()) {
            throw ApiException.badRequest("Ce prestataire ne propose pas la catégorie de service choisie.");
        }

        ServiceRequest request = ServiceRequest.submit(customerId, input.professionalId(), categoryId,
                isOther ? otherCategory : null, input.title().trim(), input.description().trim(),
                input.addressLine().trim(), input.governorateId(), input.preferredDate());
        ServiceRequest saved = serviceRequestRepository.save(request);

        return ServiceRequestResponse.from(saved, categoryName, professional.displayName());
    }
}
