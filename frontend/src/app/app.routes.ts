
import { Routes } from '@angular/router';
import { guestGuard, guestOrRoleGuard, roleGuard } from './core/auth/auth.guards';

/** data.fullscreen : page affichée sans la barre de navigation ni le pied de page. */
export const routes: Routes = [
  // Page d'accueil
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.page').then((m) => m.HomePage),
  },

  // Recherche des prestataires
  {
    path: 'prestataires',
    loadComponent: () =>
      import('./features/search/search.page').then((m) => m.SearchPage),
  },

  // Profil public d'un prestataire
  {
    path: 'prestataires/:id',
    loadComponent: () =>
      import('./features/profile/profile.page').then((m) => m.ProfilePage),
  },

  // Authentification
  {
    path: 'login',
    title: 'Connexion · NearU',
    canActivate: [guestGuard],
    data: { fullscreen: true },
    loadComponent: () =>
      import('./features/auth/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'inscription',
    title: 'Créer un compte · NearU',
    canActivate: [guestGuard],
    data: { fullscreen: true },
    loadComponent: () =>
      import('./features/auth/register/register.page').then((m) => m.RegisterPage),
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

  // Envoi d'une demande de service
  {
    path: 'demandes/nouvelle',
    title: 'Envoyer une demande · NearU',
    canActivate: [guestOrRoleGuard('CUSTOMER')],
    data: { fullscreen: true },
    loadComponent: () =>
      import('./features/service-request/send-request.page').then((m) => m.SendRequestPage),
  },

  // US02 : espace prestataire (paramètres du commerce, dashboard)
  {
    path: 'espace-prestataire',
    redirectTo: 'mon-commerce/parametres',
    pathMatch: 'full',
  },
  {
    path: 'mon-commerce/parametres',
    title: 'Paramètres du commerce · NearU',
    canActivate: [roleGuard('PROFESSIONAL')],
    loadComponent: () =>
      import('./features/settings/settings.page').then((m) => m.SettingsPage),
  },
  {
    path: 'mon-commerce/dashboard',
    title: 'Dashboard · NearU',
    canActivate: [roleGuard('PROFESSIONAL')],
    loadComponent: () =>
      import('./features/spaces/space.page').then((m) => m.SpacePage),
  },

  // Espace personnel utilisateur
  {
    path: 'account',
    loadComponent: () =>
      import('./features/account/layout/account-layout/account-layout')
        .then((m) => m.AccountLayout),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/account/pages/account/account')
            .then((m) => m.Account),
      },
      {
        path: 'location',
        loadComponent: () =>
          import('./features/account/pages/location/location')
            .then((m) => m.Location),
      },
      {
        path: 'history',
        loadComponent: () =>
          import('./features/account/pages/history/history')
            .then((m) => m.History),
      },
      {
        path: 'security',
        loadComponent: () =>
          import('./features/account/pages/security/security')
            .then((m) => m.Security),
      },
    ],
  },

  // Espace commerçant
  {
    path: 'merchant-dashboard',
    loadComponent: () =>
      import('./features/merchant/layout/merchant-layout/merchant-layout')
        .then((m) => m.MerchantLayout),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/merchant/pages/merchant-dashboard/merchant-dashboard')
            .then((m) => m.MerchantDashboard),
      },
      {
        path: 'requests',
        loadComponent: () =>
          import('./features/merchant/pages/merchant-requests/merchant-requests')
            .then((m) => m.MerchantRequests),
      },
      {
        path: 'statistics',
        loadComponent: () =>
          import('./features/merchant/pages/merchant-statistics/merchant-statistics')
            .then((m) => m.MerchantStatistics),
      },
    ],
  },

  // Route inconnue
  { path: '**', redirectTo: '' },
];
