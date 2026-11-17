import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

/** Only the given role may open the route. Anonymous visitors go to the login, other roles to their own space. */
export const roleGuard =
  (role: Role): CanActivateFn =>
  async () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    await auth.init();
    const me = auth.me();
    if (!me) {
      return router.createUrlTree(['/connexion']);
    }
    return me.role === role ? true : router.createUrlTree([auth.spacePath(me.role)]);
  };
