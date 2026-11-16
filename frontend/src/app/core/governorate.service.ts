import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Governorate {
  id: string;
  code: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class GovernorateService {
  private readonly http = inject(HttpClient);

  list(): Observable<Governorate[]> {
    return this.http.get<Governorate[]>('/api/governorates');
  }
}
