import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProviderSettings {
  name: string;
  bio: string | null;
  address: string | null;
  governorateId: string | null;
  serviceRadiusKm: number;
  phone: string | null;
  serviceIds: string[];
  visible: boolean;
  /** At least one métier and one ville (US02). */
  complete: boolean;
}

/** `name: null` keeps the current name. */
export type ProviderSettingsUpdate = Omit<ProviderSettings, 'complete' | 'name'> & { name: string | null };

@Injectable({ providedIn: 'root' })
export class ProviderSettingsService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/professional/settings';

  get(): Observable<ProviderSettings> {
    return this.http.get<ProviderSettings>(this.url);
  }

  update(body: ProviderSettingsUpdate): Observable<ProviderSettings> {
    return this.http.put<ProviderSettings>(this.url, body);
  }

  remove(): Observable<void> {
    return this.http.delete<void>(this.url);
  }
}
