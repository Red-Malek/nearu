package com.nearu.backend.servicerequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.dto.CreateServiceRequestRequest;
import com.nearu.backend.servicerequest.dto.RequestTargetResponse;
import com.nearu.backend.servicerequest.dto.RequestTargetResponse.ServiceOption;
import com.nearu.backend.servicerequest.dto.ServiceRequestResponse;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();
    private static final UUID GOVERNORATE_ID = UUID.randomUUID();
    /** « Aujourd'hui » figé : 17 novembre 2026 à 10 h, heure de Tunis. */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ServiceRequestLookupRepository lookupRepository;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-17T09:00:00Z"), ServiceRequestService.TUNIS);
        service = new ServiceRequestService(serviceRequestRepository, lookupRepository, clock);
        lenient().when(lookupRepository.findRequestTarget(PROFESSIONAL_ID)).thenReturn(Optional.of(target(true)));
        lenient().when(lookupRepository.findOfferedCategoryName(PROFESSIONAL_ID, CATEGORY_ID))
            .thenReturn(Optional.of("Plomberie"));
        lenient().when(lookupRepository.findOtherCategoryId()).thenReturn(Optional.of(OTHER_ID));
        lenient().when(lookupRepository.governorateExists(GOVERNORATE_ID)).thenReturn(true);
        lenient().when(serviceRequestRepository.save(any(ServiceRequest.class)))
            .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void urgentRequestIsPendingUrgentAndForToday() {
        ServiceRequestResponse response = service.create(CUSTOMER_ID, input(RequestTiming.URGENT, null, null));

        ServiceRequest saved = savedRequest();
        assertThat(response.status()).isEqualTo(ServiceRequestStatus.SUBMITTED);
        assertThat(response.statusLabel()).isEqualTo("En attente");
        assertThat(response.professionalName()).isEqualTo("Karim Ben Salah");
        assertThat(response.categoryName()).isEqualTo("Plomberie");
        assertThat(saved.getUrgency()).isEqualTo(Urgency.URGENT);
        assertThat(saved.getPreferredDate()).isEqualTo(TODAY);
        assertThat(saved.getPreferredTimeSlot()).isEqualTo(TimeSlot.ANY);
        assertThat(saved.getTitle()).isEqualTo("Fuite d'eau sous l'évier");
        assertThat(saved.getAddressNote()).isEqualTo("2e étage");
    }

    @Test
    void within48HoursSetsTheDeadlineTwoDaysAhead() {
        service.create(CUSTOMER_ID, input(RequestTiming.WITHIN_48H, null, null));

        assertThat(savedRequest().getPreferredDate()).isEqualTo(TODAY.plusDays(2));
        assertThat(savedRequest().getUrgency()).isEqualTo(Urgency.NORMAL);
    }

    @Test
    void scheduledRequestKeepsDateTimeAndDerivesTheSlot() {
        service.create(CUSTOMER_ID, input(RequestTiming.SCHEDULED, TODAY.plusDays(5), LocalTime.of(14, 30)));

        ServiceRequest saved = savedRequest();
        assertThat(saved.getPreferredDate()).isEqualTo(TODAY.plusDays(5));
        assertThat(saved.getPreferredTime()).isEqualTo(LocalTime.of(14, 30));
        assertThat(saved.getPreferredTimeSlot()).isEqualTo(TimeSlot.AFTERNOON);
    }

    @Test
    void scheduledRequestRequiresAFutureDate() {
        assertBadRequest(input(RequestTiming.SCHEDULED, null, null));
        assertBadRequest(input(RequestTiming.SCHEDULED, TODAY.minusDays(1), null));
    }

    @Test
    void otherServiceIsStoredWithItsPrecision() {
        CreateServiceRequestRequest other = new CreateServiceRequestRequest(PROFESSIONAL_ID, null, "  Vitrerie ",
                "Vitre cassée", "La vitre du salon est cassée.", RequestTiming.TODAY, null, null, GOVERNORATE_ID,
                "12 rue de Marseille", null, null, null);

        ServiceRequestResponse response = service.create(CUSTOMER_ID, other);

        assertThat(savedRequest().getCategoryId()).isEqualTo(OTHER_ID);
        assertThat(response.otherCategory()).isEqualTo("Vitrerie");
        assertThat(response.categoryName()).isEqualTo("Autre : Vitrerie");
    }

    @Test
    void rejectsAServiceTheProfessionalDoesNotOffer() {
        when(lookupRepository.findOfferedCategoryName(PROFESSIONAL_ID, CATEGORY_ID)).thenReturn(Optional.empty());

        assertBadRequest(input(RequestTiming.TODAY, null, null));
    }

    @Test
    void rejectsAProfessionalWhoIsNotAcceptingRequests() {
        when(lookupRepository.findRequestTarget(PROFESSIONAL_ID)).thenReturn(Optional.of(target(false)));

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, input(RequestTiming.TODAY, null, null)))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void rejectsAnUnknownProfessional() {
        when(lookupRepository.findRequestTarget(PROFESSIONAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, input(RequestTiming.TODAY, null, null)))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsAnIncompletePosition() {
        CreateServiceRequestRequest halfPosition = new CreateServiceRequestRequest(PROFESSIONAL_ID, CATEGORY_ID,
                null, "Titre", "Une description assez longue.", RequestTiming.TODAY, null, null, GOVERNORATE_ID,
                "12 rue de Marseille", null, 36.8, null);

        assertBadRequest(halfPosition);
    }

    private void assertBadRequest(CreateServiceRequestRequest request) {
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, request)).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(serviceRequestRepository, never()).save(any());
    }

    private ServiceRequest savedRequest() {
        ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(serviceRequestRepository).save(captor.capture());
        return captor.getValue();
    }

    private static RequestTargetResponse target(boolean acceptingRequests) {
        return new RequestTargetResponse(PROFESSIONAL_ID, "Karim Ben Salah", "Tunis", acceptingRequests,
                List.of(new ServiceOption(CATEGORY_ID, "Plomberie")));
    }

    private static CreateServiceRequestRequest input(RequestTiming timing, LocalDate date, LocalTime time) {
        return new CreateServiceRequestRequest(PROFESSIONAL_ID, CATEGORY_ID, null, "  Fuite d'eau sous l'évier ",
                "L'eau coule sous l'évier de la cuisine depuis hier soir.", timing, date, time, GOVERNORATE_ID,
                "12 rue de Marseille, La Marsa", " 2e étage ", 36.8782, 10.3247);
    }
}
