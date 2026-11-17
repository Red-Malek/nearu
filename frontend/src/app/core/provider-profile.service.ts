import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProviderProfile {
  id: string;
  name: string;
  bio: string | null;
  city: string | null;
  serviceRadiusKm: number;
  services: string[];
}

@Injectable({ providedIn: 'root' })
export class ProviderProfileService {
  private readonly http = inject(HttpClient);

  /** Answers 404 when the provider does not exist or is not active. */
  get(id: string): Observable<ProviderProfile> {
    return this.http.get<ProviderProfile>(`/api/providers/${encodeURIComponent(id)}`);
  }
}
