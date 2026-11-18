import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Délai souhaité (RequestTiming.java). */
export type RequestTiming =
  | 'URGENT'
  | 'TODAY'
  | 'WITHIN_48H'
  | 'SCHEDULED'
  | 'FLEXIBLE';

/** Prestataire choisi, en tête du formulaire (RequestTargetResponse.java). */
export interface RequestTarget {
  id: string;
  displayName: string;
  governorateName: string | null;
  acceptingRequests: boolean;
  services: { id: string; name: string }[];
}

/** Corps de POST /api/service-requests (CreateServiceRequestRequest.java). */
export interface CreateServiceRequest {
  professionalId: string;
  /** null quand le client choisit « Autre ». */
  categoryId: string | null;
  otherCategory: string | null;
  title: string;
  description: string;
  timing: RequestTiming;
  /** AAAA-MM-JJ, uniquement pour SCHEDULED. */
  preferredDate: string | null;
  /** HH:mm, optionnel, uniquement pour SCHEDULED. */
  preferredTime: string | null;
  governorateId: string;
  addressLine: string;
  addressNote: string | null;
  latitude: number | null;
  longitude: number | null;
}

/** Demande créée (ServiceRequestResponse.java). */
export interface ServiceRequest {
  id: string;
  title: string;
  categoryName: string;
  timing: RequestTiming;
  timingLabel: string;
  preferredDate: string | null;
  preferredTime: string | null;
  addressLine: string;
  addressNote: string | null;
  status: string;
  /** Libellé lisible, ex. « En attente ». */
  statusLabel: string;
  professionalName: string;
  createdAt: string;
}
/** Demande reçue par le prestataire (ReceivedRequestResponse.java). */
export interface ReceivedRequest {
  id: string;
  customerName: string;
  title: string;
  description: string;
  serviceName: string;
  timing: RequestTiming;
  timingLabel: string;
  urgent: boolean;
  preferredDate: string | null;
  preferredTime: string | null;
  governorateName: string;
  addressLine: string;
  addressNote: string | null;
  latitude: number | null;
  longitude: number | null;
  status: string;
  statusLabel: string;
  createdAt: string;
}
/** Informations envoyées pour créer un devis. */
export interface CreateQuote {
  amount: number;
  proposedStart: string;
  estimatedDurationMinutes: number;
  validUntil: string;
}
export interface QuoteResponse {
  id: string;
  requestId: string;
  amount: number;
  proposedStart: string;
  estimatedDurationMinutes: number;
  validUntil: string;
  status: string;
  version: number;
  createdAt: string;
}
/** Limites identiques aux validations du backend. */
export const REQUEST_LIMITS = {
  titleMax: 80,
  descriptionMin: 10,
  descriptionMax: 1000,
  addressMax: 255,
  addressNoteMax: 200,
  otherCategoryMin: 2,
  otherCategoryMax: 80,
} as const;

@Injectable({ providedIn: 'root' })
export class ServiceRequestService {
  private readonly http = inject(HttpClient);

  /** Prestataire et ses services (public : le visiteur voit le formulaire avant de se connecter). */
  getTarget(professionalId: string): Observable<RequestTarget> {
    return this.http.get<RequestTarget>(`/api/service-requests/target/${encodeURIComponent(professionalId)}`);
  }

  create(request: CreateServiceRequest): Observable<ServiceRequest> {
    return this.http.post<ServiceRequest>('/api/service-requests', request);
  }
  getReceivedRequests(): Observable<ReceivedRequest[]> {
  return this.http.get<ReceivedRequest[]>('/api/service-requests/received');
}
rejectRequest(requestId: string): Observable<void> {
  return this.http.patch<void>(
    `/api/service-requests/${encodeURIComponent(requestId)}/reject`,
    {}
  );
}
createQuote(requestId: string, quote: CreateQuote): Observable<void> {
  return this.http.post<void>(
    `/api/service-requests/${encodeURIComponent(requestId)}/quotes`,
    quote
  );
}
getMyQuotes(): Observable<QuoteResponse[]> {
  return this.http.get<QuoteResponse[]>('/api/service-requests/quotes');
}

}
