import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Prestataire pouvant recevoir une demande (EligibleProfessionalResponse.java). */
export interface EligibleProfessional {
  id: string;
  displayName: string;
  governorateName: string | null;
  avgRating: number;
  reviewCount: number;
  verified: boolean;
}

/** Corps de POST /api/service-requests (CreateServiceRequestRequest.java). */
export interface CreateServiceRequest {
  /** null quand le client choisit « Autre ». */
  categoryId: string | null;
  /** Catégorie précisée par le client quand il choisit « Autre ». */
  otherCategory: string | null;
  title: string;
  description: string;
  governorateId: string;
  addressLine: string;
  /** AAAA-MM-JJ, ou null. */
  preferredDate: string | null;
  professionalId: string;
}

/** Demande créée (ServiceRequestResponse.java). */
export interface ServiceRequest {
  id: string;
  title: string;
  description: string;
  addressLine: string;
  preferredDate: string | null;
  status: string;
  /** Libellé lisible, ex. « En attente ». */
  statusLabel: string;
  categoryId: string;
  categoryName: string;
  otherCategory: string | null;
  governorateId: string;
  professionalId: string;
  professionalName: string;
  createdAt: string;
}

/** Limites identiques aux validations du backend. */
export const REQUEST_LIMITS = {
  titleMax: 120,
  descriptionMin: 10,
  descriptionMax: 1000,
  addressMax: 255,
  otherCategoryMin: 2,
  otherCategoryMax: 80,
} as const;

@Injectable({ providedIn: 'root' })
export class ServiceRequestService {
  private readonly http = inject(HttpClient);

  /** categoryId null = catégorie « Autre » : tous les prestataires disponibles. */
  findEligibleProfessionals(
    categoryId: string | null,
    governorateId: string | null,
    query: string,
  ): Observable<EligibleProfessional[]> {
    let params = new HttpParams();
    if (categoryId) params = params.set('categoryId', categoryId);
    if (governorateId) params = params.set('governorateId', governorateId);
    if (query.trim()) params = params.set('q', query.trim());
    return this.http.get<EligibleProfessional[]>('/api/service-requests/eligible-professionals', { params });
  }

  create(request: CreateServiceRequest): Observable<ServiceRequest> {
    return this.http.post<ServiceRequest>('/api/service-requests', request);
  }
}
