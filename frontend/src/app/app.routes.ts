import { Routes } from '@angular/router';

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
  { path: '**', redirectTo: '' },
];
