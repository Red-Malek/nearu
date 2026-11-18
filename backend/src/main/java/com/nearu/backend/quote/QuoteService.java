
package com.nearu.backend.quote;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.ServiceRequest;
import com.nearu.backend.servicerequest.ServiceRequestRepository;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public QuoteService(
            QuoteRepository quoteRepository,
            ServiceRequestRepository serviceRequestRepository) {
        this.quoteRepository = quoteRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional
    public Quote create(UUID professionalId, UUID requestId, CreateQuoteRequest input) {

        ServiceRequest request = serviceRequestRepository.findById(requestId)
            .orElseThrow(() -> ApiException.notFound("Demande introuvable."));

        if (!request.getProfessionalId().equals(professionalId)) {
            throw ApiException.notFound("Demande introuvable.");
        }

        try {
    request.addQuote();
} catch (IllegalStateException ex) {
    throw ApiException.conflict(ex.getMessage());
}

        Instant now = Instant.now();

        if (!input.proposedStart().isAfter(now)) {
            throw ApiException.badRequest("La date de début doit être dans le futur.");
        }

        if (!input.validUntil().isAfter(now)) {
            throw ApiException.badRequest("La date de validité doit être dans le futur.");
        }

        if (!input.validUntil().isAfter(input.proposedStart())) {
            throw ApiException.badRequest("La validité du devis doit dépasser la date de début proposée.");
        }

        Quote quote = Quote.create(requestId, professionalId, input);

        return quoteRepository.save(quote);
    }
    @Transactional(readOnly = true)
public List<QuoteResponse> getMyQuotes(UUID professionalId) {
    return quoteRepository
        .findByProfessionalIdOrderByCreatedAtDesc(professionalId)
        .stream()
        .map(quote -> new QuoteResponse(
            quote.getId(),
            quote.getRequestId(),
            quote.getAmount(),
            quote.getProposedStart(),
            quote.getEstimatedDurationMinutes(),
            quote.getValidUntil(),
            quote.getStatus(),
            quote.getVersion(),
            quote.getCreatedAt()
        ))
        .toList();
}
}
