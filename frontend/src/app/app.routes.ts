import { Routes } from '@angular/router';
import { guestGuard, guestOrRoleGuard } from './core/auth/auth.guards';

/** data.fullscreen : page affichée sans la barre de navigation ni le pied de page (écrans de connexion). */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage) },
  {
    path: 'prestataires',
    loadComponent: () => import('./features/search/search.page').then((m) => m.SearchPage),
  },
  {
    path: 'prestataires/:id',
    loadComponent: () => import('./features/profile/profile.page').then((m) => m.ProfilePage),
  },
  {
    path: 'login',
    title: 'Connexion · NearU',
    canActivate: [guestGuard],
    data: { fullscreen: true },
    loadComponent: () => import('./features/auth/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'inscription',
    title: 'Créer un compte · NearU',
    canActivate: [guestGuard],
    data: { fullscreen: true },
    loadComponent: () => import('./features/auth/register/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'forgot-password',
    title: 'Mot de passe oublié · NearU',
    canActivate: [guestGuard],
    data: { fullscreen: true },
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.page').then((m) => m.ForgotPasswordPage),
  },
  {
    path: 'reset-password',
    title: 'Nouveau mot de passe · NearU',
    data: { fullscreen: true },
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.page').then((m) => m.ResetPasswordPage),
  },
  {
    path: 'demandes/nouvelle',
    title: 'Envoyer une demande · NearU',
    // US06 : un visiteur voit le formulaire mais doit se connecter pour l'envoyer
    canActivate: [guestOrRoleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/service-request/send-request.page').then((m) => m.SendRequestPage),
  },
  { path: '**', redirectTo: '' },
];
