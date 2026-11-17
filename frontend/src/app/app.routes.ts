import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

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
    path: 'inscription',
    loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'connexion',
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'espace-client',
    canActivate: [roleGuard('CUSTOMER')],
    loadComponent: () => import('./features/spaces/space.page').then((m) => m.SpacePage),
  },
  {
    path: 'espace-prestataire',
    canActivate: [roleGuard('PROFESSIONAL')],
    loadComponent: () => import('./features/spaces/space.page').then((m) => m.SpacePage),
  },
  { path: '**', redirectTo: '' },
];
