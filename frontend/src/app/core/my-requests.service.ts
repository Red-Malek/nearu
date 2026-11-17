import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Statuts d'une demande (ServiceRequestStatus.java). */
export type RequestStatus =
  | 'SUBMITTED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'QUOTED'
  | 'QUOTE_ACCEPTED'
  | 'QUOTE_REJECTED'
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'EXPIRED';

/** Une ligne de « Mes demandes » (MyRequestSummary.java). */
export interface MyRequestSummary {
  id: string;
  title: string;
  serviceName: string;
  professionalId: string;
  professionalName: string;
  status: RequestStatus;
  /** Libellé en toutes lettres (US10, critère 1). */
  statusLabel: string;
  quoteAmount: number | null;
  createdAt: string;
  updatedAt: string;
}

/** Devis reçu (QuoteView.java). */
export interface QuoteView {
  id: string;
  amount: number;
  description: string | null;
  proposedStart: string;
  estimatedDurationMinutes: number;
  validUntil: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN' | 'EXPIRED' | 'CANCELLED';
  statusLabel: string;
  expired: boolean;
  /** Vrai seulement si le client peut encore accepter ou refuser. */
  canDecide: boolean;
  decidedAt: string | null;
  createdAt: string;
}

/** Détail d'une demande avec tous ses devis, du plus récent au plus ancien (MyRequestDetail.java). */
export interface MyRequestDetail {
  id: string;
  title: string;
  description: string;
  serviceName: string;
  timing: string;
  timingLabel: string;
  preferredDate: string | null;
  preferredTime: string | null;
  governorateName: string;
  addressLine: string;
  addressNote: string | null;
  status: RequestStatus;
  statusLabel: string;
  professionalId: string;
  professionalName: string;
  createdAt: string;
  updatedAt: string;
  quotes: QuoteView[];
}

/** Suivi des demandes (US10) et décision sur le devis (US09), client connecté. */
@Injectable({ providedIn: 'root' })
export class MyRequestsService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/service-requests/mine';

  list(): Observable<MyRequestSummary[]> {
    return this.http.get<MyRequestSummary[]>(this.url);
  }

  get(id: string): Observable<MyRequestDetail> {
    return this.http.get<MyRequestDetail>(`${this.url}/${encodeURIComponent(id)}`);
  }

  acceptQuote(requestId: string, quoteId: string): Observable<MyRequestDetail> {
    return this.http.post<MyRequestDetail>(this.quoteUrl(requestId, quoteId, 'accept'), {});
  }

  rejectQuote(requestId: string, quoteId: string): Observable<MyRequestDetail> {
    return this.http.post<MyRequestDetail>(this.quoteUrl(requestId, quoteId, 'reject'), {});
  }

  private quoteUrl(requestId: string, quoteId: string, decision: 'accept' | 'reject'): string {
    return `${this.url}/${encodeURIComponent(requestId)}/quotes/${encodeURIComponent(quoteId)}/${decision}`;
  }
}

/** Ton de la pastille de statut ; le libellé reste toujours écrit en toutes lettres. */
export function statusTone(status: RequestStatus): 'wait' | 'action' | 'ok' | 'ko' | 'neutral' {
  switch (status) {
    case 'SUBMITTED':
    case 'ACCEPTED':
      return 'wait';
    case 'QUOTED':
      return 'action';
    case 'QUOTE_ACCEPTED':
    case 'SCHEDULED':
    case 'COMPLETED':
      return 'ok';
    case 'REJECTED':
    case 'QUOTE_REJECTED':
    case 'CANCELLED':
    case 'EXPIRED':
      return 'ko';
    default:
      return 'neutral';
  }
}
