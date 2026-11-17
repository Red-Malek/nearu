import { Routes } from '@angular/router';
import { guestGuard, guestOrRoleGuard, roleGuard } from './core/auth/auth.guards';

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
    // US06 : ouvert depuis le profil d'un prestataire (?professionalId=…). Un visiteur voit le
    // formulaire mais doit se connecter pour l'envoyer. Écran plein (en-tête propre à la maquette).
    canActivate: [guestOrRoleGuard('CUSTOMER')],
    data: { fullscreen: true },
    loadComponent: () =>
      import('./features/service-request/send-request.page').then((m) => m.SendRequestPage),
  },
  // US10 / US09 : suivi des demandes du client et décision sur le devis
  {
    path: 'mes-demandes',
    title: 'Mes demandes · NearU',
    canActivate: [roleGuard('CUSTOMER')],
    loadComponent: () => import('./features/my-requests/my-requests.page').then((m) => m.MyRequestsPage),
  },
  {
    path: 'mes-demandes/:id',
    title: 'Détail de la demande · NearU',
    canActivate: [roleGuard('CUSTOMER')],
    loadComponent: () =>
      import('./features/my-requests/request-detail.page').then((m) => m.RequestDetailPage),
  },
  // US02 : espace prestataire (paramètres du commerce, dashboard)
  { path: 'espace-prestataire', redirectTo: 'mon-commerce/parametres', pathMatch: 'full' },
  {
    path: 'mon-commerce/parametres',
    title: 'Paramètres du commerce · NearU',
    canActivate: [roleGuard('PROFESSIONAL')],
    loadComponent: () => import('./features/settings/settings.page').then((m) => m.SettingsPage),
  },
  {
    path: 'mon-commerce/dashboard',
    title: 'Dashboard · NearU',
    canActivate: [roleGuard('PROFESSIONAL')],
    loadComponent: () => import('./features/spaces/space.page').then((m) => m.SpacePage),
  },
  { path: '**', redirectTo: '' },
];
