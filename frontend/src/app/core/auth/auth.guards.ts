import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ROLE_HOME, UserRole } from './auth.models';
import { AuthService } from './auth.service';

/**
 * Réserve une page à un rôle (US01, critère 3).
 * Visiteur non connecté : redirigé vers /login, puis ramené sur la page demandée.
 * Utilisateur d'un autre rôle : renvoyé vers la page d'accueil de son rôle.
 */
export function roleGuard(role: UserRole): CanActivateFn {
  return async (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = await auth.ensureCurrentUser();
    if (!user) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    return user.role === role ? true : router.parseUrl(ROLE_HOME[user.role]);
  };
}

/** Pages réservées aux visiteurs (connexion, mot de passe oublié). */
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = await auth.ensureCurrentUser();
  return user ? router.parseUrl(ROLE_HOME[user.role]) : true;
};

/**
 * Page visible par un visiteur (qui sera invité à se connecter pour agir) et par le rôle
 * demandé ; un utilisateur connecté avec un autre rôle est renvoyé vers sa page d'accueil.
 */
export function guestOrRoleGuard(role: UserRole): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = await auth.ensureCurrentUser();
    return !user || user.role === role ? true : router.parseUrl(ROLE_HOME[user.role]);
  };
}
