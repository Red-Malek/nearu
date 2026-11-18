import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'prestataires', renderMode: RenderMode.Client },
  { path: 'prestataires/:id', renderMode: RenderMode.Client },
  // Pages liées à la session de l'utilisateur : rendues uniquement dans le navigateur
  { path: 'login', renderMode: RenderMode.Client },
  { path: 'inscription', renderMode: RenderMode.Client },
  { path: 'forgot-password', renderMode: RenderMode.Client },
  { path: 'reset-password', renderMode: RenderMode.Client },
  { path: 'demandes/nouvelle', renderMode: RenderMode.Client },
  { path: 'mon-commerce/parametres', renderMode: RenderMode.Client },
  { path: 'mon-commerce/dashboard', renderMode: RenderMode.Client },
  // Pages personnelles et espace commerçant
{ path: 'account', renderMode: RenderMode.Client },
{ path: 'account/**', renderMode: RenderMode.Client },
{ path: 'merchant-dashboard', renderMode: RenderMode.Client },
{ path: 'merchant-dashboard/**', renderMode: RenderMode.Client },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
