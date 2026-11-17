package com.nearu.backend.servicerequest.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.RequestTiming;
import com.nearu.backend.servicerequest.ServiceRequestStatus;
import com.nearu.backend.servicerequest.tracking.CustomerRequestRepository.QuoteDecisionRow;
import com.nearu.backend.servicerequest.tracking.CustomerRequestRepository.QuoteRow;
import com.nearu.backend.servicerequest.tracking.dto.MyRequestDetail;

@ExtendWith(MockitoExtension.class)
class CustomerRequestServiceTest {

    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID REQUEST = UUID.randomUUID();
    private static final UUID QUOTE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-18T09:00:00Z");

    @Mock
    private CustomerRequestRepository repository;

    private CustomerRequestService service;

    @BeforeEach
    void setUp() {
        service = new CustomerRequestService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void acceptingThePendingQuoteMovesTheRequestToQuoteAccepted() {
        givenDecision(ServiceRequestStatus.QUOTED, QuoteStatus.PENDING, NOW.plusSeconds(86_400));
        when(repository.decideQuote(QUOTE, QuoteStatus.ACCEPTED)).thenReturn(1);
        when(repository.changeRequestStatus(REQUEST, ServiceRequestStatus.QUOTED, ServiceRequestStatus.QUOTE_ACCEPTED))
            .thenReturn(1);
        givenDetail(ServiceRequestStatus.QUOTE_ACCEPTED, QuoteStatus.ACCEPTED);

        MyRequestDetail detail = service.decide(CUSTOMER, REQUEST, QUOTE, true);

        assertThat(detail.statusLabel()).isEqualTo("Devis accepté");
        assertThat(detail.quotes()).singleElement().satisfies(q -> {
            assertThat(q.canDecide()).isFalse();
            assertThat(q.statusLabel()).isEqualTo("Devis accepté");
        });
        verify(repository).recordHistory("SERVICE_REQUEST", REQUEST, "QUOTED", "QUOTE_ACCEPTED", CUSTOMER);
    }

    @Test
    void refusingThePendingQuoteMovesTheRequestToQuoteRejected() {
        givenDecision(ServiceRequestStatus.QUOTED, QuoteStatus.PENDING, NOW.plusSeconds(86_400));
        when(repository.decideQuote(QUOTE, QuoteStatus.REJECTED)).thenReturn(1);
        when(repository.changeRequestStatus(REQUEST, ServiceRequestStatus.QUOTED, ServiceRequestStatus.QUOTE_REJECTED))
            .thenReturn(1);
        givenDetail(ServiceRequestStatus.QUOTE_REJECTED, QuoteStatus.REJECTED);

        assertThat(service.decide(CUSTOMER, REQUEST, QUOTE, false).statusLabel()).isEqualTo("Devis refusé");
    }

    @Test
    void aDecisionIsDefinitive() {
        givenDecision(ServiceRequestStatus.QUOTE_ACCEPTED, QuoteStatus.ACCEPTED, NOW.plusSeconds(86_400));

        assertConflict(() -> service.decide(CUSTOMER, REQUEST, QUOTE, false));
        verify(repository, never()).decideQuote(any(), any());
    }

    @Test
    void anExpiredQuoteCannotBeAccepted() {
        givenDecision(ServiceRequestStatus.QUOTED, QuoteStatus.PENDING, NOW.minusSeconds(60));

        assertConflict(() -> service.decide(CUSTOMER, REQUEST, QUOTE, true));
        verify(repository, never()).decideQuote(any(), any());
    }

    @Test
    void aQuoteOfAnotherRequestIsNotFound() {
        when(repository.lockRequest(CUSTOMER, REQUEST)).thenReturn(Optional.of(ServiceRequestStatus.QUOTED));
        when(repository.lockQuote(REQUEST, QUOTE)).thenReturn(Optional.empty());

        assertNotFound(() -> service.decide(CUSTOMER, REQUEST, QUOTE, true));
        verify(repository, never()).decideQuote(any(), any());
    }

    @Test
    void aRequestThatNoLongerWaitsForAnAnswerCannotBeDecided() {
        givenDecision(ServiceRequestStatus.CANCELLED, QuoteStatus.PENDING, NOW.plusSeconds(86_400));

        assertConflict(() -> service.decide(CUSTOMER, REQUEST, QUOTE, true));
        verify(repository, never()).decideQuote(any(), any());
    }

    @Test
    void aConcurrentDecisionIsRejected() {
        givenDecision(ServiceRequestStatus.QUOTED, QuoteStatus.PENDING, NOW.plusSeconds(86_400));
        when(repository.decideQuote(QUOTE, QuoteStatus.ACCEPTED)).thenReturn(0);

        assertConflict(() -> service.decide(CUSTOMER, REQUEST, QUOTE, true));
        verify(repository, never()).recordHistory(anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void anotherCustomersRequestIsNotFound() {
        when(repository.lockRequest(CUSTOMER, REQUEST)).thenReturn(Optional.empty());

        assertNotFound(() -> service.decide(CUSTOMER, REQUEST, QUOTE, true));
        verify(repository, never()).lockQuote(any(), any());
        verify(repository, never()).recordHistory(anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void allQuotesOfTheRequestAreShownAndOnlyTheValidPendingOneCanBeDecided() {
        UUID oldQuote = UUID.randomUUID();
        when(repository.findMineDetail(CUSTOMER, REQUEST)).thenReturn(Optional.of(request(ServiceRequestStatus.QUOTED)));
        when(repository.findQuotes(REQUEST)).thenReturn(List.of(quote(QUOTE, QuoteStatus.PENDING, NOW.plusSeconds(60)),
                quote(oldQuote, QuoteStatus.WITHDRAWN, NOW.plusSeconds(60))));

        MyRequestDetail detail = service.getMine(CUSTOMER, REQUEST);

        assertThat(detail.quotes()).hasSize(2);
        assertThat(detail.quotes().get(0).canDecide()).isTrue();
        assertThat(detail.quotes().get(1).canDecide()).isFalse();
    }

    @Test
    void anExpiredPendingQuoteIsShownAsExpiredWithoutDecision() {
        when(repository.findMineDetail(CUSTOMER, REQUEST)).thenReturn(Optional.of(request(ServiceRequestStatus.QUOTED)));
        when(repository.findQuotes(REQUEST)).thenReturn(List.of(quote(QUOTE, QuoteStatus.PENDING, NOW.minusSeconds(1))));

        MyRequestDetail detail = service.getMine(CUSTOMER, REQUEST);

        assertThat(detail.quotes()).singleElement().satisfies(q -> {
            assertThat(q.expired()).isTrue();
            assertThat(q.canDecide()).isFalse();
            assertThat(q.statusLabel()).isEqualTo("Expiré");
        });
    }

    @Test
    void anotherCustomersRequestDetailIsNotFound() {
        when(repository.findMineDetail(CUSTOMER, REQUEST)).thenReturn(Optional.empty());

        assertNotFound(() -> service.getMine(CUSTOMER, REQUEST));
        verify(repository, never()).findQuotes(any());
    }

    private void givenDecision(ServiceRequestStatus requestStatus, QuoteStatus quoteStatus, Instant validUntil) {
        when(repository.lockRequest(CUSTOMER, REQUEST)).thenReturn(Optional.of(requestStatus));
        when(repository.lockQuote(REQUEST, QUOTE)).thenReturn(Optional.of(new QuoteDecisionRow(quoteStatus, validUntil)));
    }

    private void givenDetail(ServiceRequestStatus requestStatus, QuoteStatus quoteStatus) {
        when(repository.findMineDetail(CUSTOMER, REQUEST)).thenReturn(Optional.of(request(requestStatus)));
        when(repository.findQuotes(REQUEST)).thenReturn(List.of(quote(QUOTE, quoteStatus, NOW.plusSeconds(86_400))));
    }

    private void assertConflict(Runnable action) {
        assertStatus(action, HttpStatus.CONFLICT);
    }

    private void assertNotFound(Runnable action) {
        assertStatus(action, HttpStatus.NOT_FOUND);
    }

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(status));
    }

    private static QuoteRow quote(UUID id, QuoteStatus status, Instant validUntil) {
        return new QuoteRow(id, new BigDecimal("120.000"), "Remplacement du siphon", NOW.plusSeconds(172_800), 90,
                validUntil, status, null, NOW.minusSeconds(3_600));
    }

    private static MyRequestDetail request(ServiceRequestStatus status) {
        return new MyRequestDetail(REQUEST, "Fuite d'eau", "L'évier fuit.", "Plomberie", RequestTiming.TODAY,
                RequestTiming.TODAY.getLabel(), null, null, "Tunis", "12 rue de Marseille", null, status,
                status.getLabel(), UUID.randomUUID(), "Karim Jlassi", NOW, NOW, List.of());
    }
}
