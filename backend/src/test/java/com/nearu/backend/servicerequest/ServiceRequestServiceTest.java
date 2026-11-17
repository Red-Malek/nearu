package com.nearu.backend.servicerequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.nearu.backend.common.error.ApiException;
import com.nearu.backend.servicerequest.ServiceRequestLookupRepository.ProfessionalTarget;
import com.nearu.backend.servicerequest.dto.CreateServiceRequestRequest;
import com.nearu.backend.servicerequest.dto.ServiceRequestResponse;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private static final UUID PROFESSIONAL_ID = UUID.randomUUID();

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private static final UUID GOVERNORATE_ID = UUID.randomUUID();

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ServiceRequestLookupRepository lookupRepository;

    private ServiceRequestService service;

    @BeforeEach
    void setUp() {
        service = new ServiceRequestService(serviceRequestRepository, lookupRepository);
    }

    @Test
    void createsRequestWithPendingStatus() {
        givenValidCategoryAndGovernorate();
        when(lookupRepository.findProfessionalTarget(PROFESSIONAL_ID, CATEGORY_ID))
            .thenReturn(Optional.of(new ProfessionalTarget("Amine Services", true, true, true)));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(call -> call.getArgument(0));

        ServiceRequestResponse response = service.create(CUSTOMER_ID, validInput());

        assertThat(response.status()).isEqualTo(ServiceRequestStatus.SUBMITTED);
        assertThat(response.statusLabel()).isEqualTo("En attente");
        assertThat(response.title()).isEqualTo("Fuite sous l'évier");
        assertThat(response.professionalName()).isEqualTo("Amine Services");
        assertThat(response.categoryName()).isEqualTo("Plomberie");
    }

    @Test
    void rejectsProfessionalWhoDoesNotOfferCategory() {
        givenValidCategoryAndGovernorate();
        when(lookupRepository.findProfessionalTarget(PROFESSIONAL_ID, CATEGORY_ID))
            .thenReturn(Optional.of(new ProfessionalTarget("Amine Services", true, true, false)));

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, validInput())).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void rejectsProfessionalNotAcceptingRequests() {
        givenValidCategoryAndGovernorate();
        when(lookupRepository.findProfessionalTarget(PROFESSIONAL_ID, CATEGORY_ID))
            .thenReturn(Optional.of(new ProfessionalTarget("Amine Services", false, true, true)));

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, validInput())).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownProfessional() {
        givenValidCategoryAndGovernorate();
        when(lookupRepository.findProfessionalTarget(PROFESSIONAL_ID, CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, validInput())).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsUnknownCategory() {
        when(lookupRepository.findActiveCategoryName(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, validInput())).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void createsRequestWithOtherCategory() {
        UUID otherId = UUID.randomUUID();
        when(lookupRepository.findOtherCategoryId()).thenReturn(Optional.of(otherId));
        when(lookupRepository.governorateExists(GOVERNORATE_ID)).thenReturn(true);
        when(lookupRepository.findProfessionalTarget(PROFESSIONAL_ID, null))
            .thenReturn(Optional.of(new ProfessionalTarget("Amine Services", true, true, true)));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(call -> call.getArgument(0));

        ServiceRequestResponse response = service.create(CUSTOMER_ID, otherInput("  Vitrerie "));

        assertThat(response.categoryId()).isEqualTo(otherId);
        assertThat(response.otherCategory()).isEqualTo("Vitrerie");
        assertThat(response.categoryName()).isEqualTo("Autre : Vitrerie");
        assertThat(response.status()).isEqualTo(ServiceRequestStatus.SUBMITTED);
    }

    @Test
    void rejectsOtherCategoryWithoutPrecision() {
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, otherInput(" "))).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void rejectsListCategoryCombinedWithOtherPrecision() {
        CreateServiceRequestRequest both = new CreateServiceRequestRequest(CATEGORY_ID, "Vitrerie", "Titre",
                "Une description assez longue.", GOVERNORATE_ID, "12 rue de Marseille", null, PROFESSIONAL_ID);

        assertThatThrownBy(() -> service.create(CUSTOMER_ID, both)).isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static CreateServiceRequestRequest otherInput(String otherCategory) {
        return new CreateServiceRequestRequest(null, otherCategory, "Vitre cassée",
                "La vitre de la fenêtre du salon est cassée.", GOVERNORATE_ID, "12 rue de Marseille", null,
                PROFESSIONAL_ID);
    }

    private void givenValidCategoryAndGovernorate() {
        when(lookupRepository.findActiveCategoryName(CATEGORY_ID)).thenReturn(Optional.of("Plomberie"));
        when(lookupRepository.governorateExists(GOVERNORATE_ID)).thenReturn(true);
    }

    private static CreateServiceRequestRequest validInput() {
        return new CreateServiceRequestRequest(CATEGORY_ID, null, "  Fuite sous l'évier ",
                "L'eau coule sous l'évier de la cuisine depuis hier soir.", GOVERNORATE_ID, "12 rue de Marseille",
                LocalDate.now().plusDays(2), PROFESSIONAL_ID);
    }
}
