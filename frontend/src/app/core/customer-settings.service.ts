import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CustomerProfile {
  firstName: string;
  lastName: string;
  /** Lecture seule : géré par Supabase Auth. */
  email: string | null;
  phone: string | null;
}

/** `phone: null` ou vide garde le numéro actuel. */
export type CustomerProfileUpdate = Omit<CustomerProfile, 'email'>;

export interface CustomerLocation {
  address: string | null;
  postalCode: string | null;
  city: string | null;
  searchRadiusKm: number;
  useGps: boolean;
  hideExactAddress: boolean;
}

@Injectable({ providedIn: 'root' })
export class CustomerSettingsService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/customer/settings';

  getProfile(): Observable<CustomerProfile> {
    return this.http.get<CustomerProfile>(`${this.url}/profile`);
  }

  updateProfile(body: CustomerProfileUpdate): Observable<CustomerProfile> {
    return this.http.put<CustomerProfile>(`${this.url}/profile`, body);
  }

  getLocation(): Observable<CustomerLocation> {
    return this.http.get<CustomerLocation>(`${this.url}/location`);
  }

  updateLocation(body: CustomerLocation): Observable<CustomerLocation> {
    return this.http.put<CustomerLocation>(`${this.url}/location`, body);
  }

  remove(): Observable<void> {
    return this.http.delete<void>(this.url);
  }
}
