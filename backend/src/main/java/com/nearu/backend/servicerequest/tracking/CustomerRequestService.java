package com.nearu.backend.servicerequest.tracking;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.ServiceRequestStatus;
import com.nearu.backend.servicerequest.tracking.CustomerRequestRepository.QuoteDecisionRow;
import com.nearu.backend.servicerequest.tracking.CustomerRequestRepository.QuoteRow;
import com.nearu.backend.servicerequest.tracking.dto.MyRequestDetail;
import com.nearu.backend.servicerequest.tracking.dto.MyRequestSummary;
import com.nearu.backend.servicerequest.tracking.dto.QuoteView;

/**
 * Espace « Mes demandes » du client : suivi (US10) et décision sur le devis reçu (US09).
 * <p>
 * Règles US09 : le client ne voit que les devis de ses propres demandes ; accepter un devis fait passer la
 * demande à « Devis accepté », refuser à « Devis refusé » ; la décision est définitive et visible du
 * prestataire (le statut de la demande change dans GET /api/service-requests/received).
 */
@Service
public class CustomerRequestService {

    private static final String NOT_FOUND = "Cette demande est introuvable.";

    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm").withZone(ZoneId.of("Africa/Tunis"));

    private final CustomerRequestRepository repository;

    private final Clock clock;

    @Autowired
    public CustomerRequestService(CustomerRequestRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CustomerRequestService(CustomerRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MyRequestSummary> listMine(UUID customerId) {
        return repository.findMine(customerId);
    }

    @Transactional(readOnly = true)
    public MyRequestDetail getMine(UUID customerId, UUID requestId) {
        MyRequestDetail request = repository.findMineDetail(customerId, requestId)
            .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        List<QuoteView> quotes = repository.findQuotes(requestId).stream()
            .map(quote -> toView(quote, request.status()))
            .toList();
        return request.withQuotes(quotes);
    }

    /**
     * Accepte ou refuse un devis en attente d'une demande du client.
     *
     * @param accept true pour accepter, false pour refuser
     */
    @Transactional
    public MyRequestDetail decide(UUID customerId, UUID requestId, UUID quoteId, boolean accept) {
        ServiceRequestStatus requestStatus = repository.lockRequest(customerId, requestId)
            .orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        QuoteDecisionRow quote = repository.lockQuote(requestId, quoteId)
            .orElseThrow(() -> ApiException.notFound("Ce devis est introuvable."));

        if (quote.status() == QuoteStatus.ACCEPTED || quote.status() == QuoteStatus.REJECTED) {
            throw ApiException.conflict("Vous avez déjà répondu à ce devis : la décision est définitive.");
        }
        if (quote.status() != QuoteStatus.PENDING) {
            throw ApiException.conflict("Ce devis n'est plus disponible : " + quote.status().getLabel()
                .toLowerCase() + ".");
        }
        if (requestStatus != ServiceRequestStatus.QUOTED) {
            throw ApiException.conflict("Cette demande n'attend plus de réponse (statut : "
                    + requestStatus.getLabel() + ").");
        }
        if (quote.validUntil() != null && !quote.validUntil().isAfter(Instant.now(clock))) {
            throw ApiException.conflict("Ce devis a expiré le " + DATE_FR.format(quote.validUntil())
                    + ". Demandez un nouveau devis au prestataire.");
        }

        QuoteStatus quoteDecision = accept ? QuoteStatus.ACCEPTED : QuoteStatus.REJECTED;
        ServiceRequestStatus requestDecision = accept ? ServiceRequestStatus.QUOTE_ACCEPTED
                : ServiceRequestStatus.QUOTE_REJECTED;

        if (repository.decideQuote(quoteId, quoteDecision) != 1
                || repository.changeRequestStatus(requestId, ServiceRequestStatus.QUOTED, requestDecision) != 1) {
            throw ApiException.conflict("Ce devis vient d'être modifié. Rechargez la page.");
        }
        repository.recordHistory("QUOTE", quoteId, QuoteStatus.PENDING.name(), quoteDecision.name(), customerId);
        repository.recordHistory("SERVICE_REQUEST", requestId, ServiceRequestStatus.QUOTED.name(),
                requestDecision.name(), customerId);

        return getMine(customerId, requestId);
    }

    private QuoteView toView(QuoteRow quote, ServiceRequestStatus requestStatus) {
        boolean expired = quote.status() == QuoteStatus.PENDING && quote.validUntil() != null
                && !quote.validUntil().isAfter(Instant.now(clock));
        boolean canDecide = quote.status() == QuoteStatus.PENDING && requestStatus == ServiceRequestStatus.QUOTED
                && !expired;
        QuoteStatus shown = expired ? QuoteStatus.EXPIRED : quote.status();
        return new QuoteView(quote.id(), quote.amount(), quote.description(), quote.proposedStart(),
                quote.estimatedDurationMinutes(), quote.validUntil(), shown, shown.getLabel(), expired, canDecide,
                quote.decidedAt(), quote.createdAt());
    }
}
