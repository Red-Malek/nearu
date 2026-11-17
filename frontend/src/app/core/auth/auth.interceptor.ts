import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Ajoute « Authorization: Bearer <jeton Supabase> » aux appels vers notre API (/api/...),
 * jamais vers un site tiers.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/') || req.url.startsWith('/api/public-config')) {
    return next(req);
  }
  const auth = inject(AuthService);
  return from(auth.getAccessToken()).pipe(
    switchMap((token) =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
    ),
  );
};
