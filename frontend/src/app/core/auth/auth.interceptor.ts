import { HttpInterceptorFn } from '@angular/common/http';
import { PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

/** Adds the Supabase access token to calls to our own API (browser only). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isPlatformBrowser(inject(PLATFORM_ID)) || !req.url.startsWith('/api/')) {
    return next(req);
  }
  return from(inject(AuthService).token()).pipe(
    switchMap((token) =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
    ),
  );
};
