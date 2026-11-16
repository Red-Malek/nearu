import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProviderSearchResult {
  id: string;
  name: string;
  city: string;
  services: string[];
}

@Injectable({ providedIn: 'root' })
export class ProviderSearchService {
  private readonly http = inject(HttpClient);

  /** category = category slug, city = governorate code; empty means "no filter". */
  search(category: string | null, city: string | null): Observable<ProviderSearchResult[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    if (city) params = params.set('city', city);
    return this.http.get<ProviderSearchResult[]>('/api/providers', { params });
  }
}
