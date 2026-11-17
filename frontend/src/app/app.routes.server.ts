import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'prestataires', renderMode: RenderMode.Client },
  { path: 'prestataires/:id', renderMode: RenderMode.Client },
  { path: 'inscription', renderMode: RenderMode.Client },
  { path: 'connexion', renderMode: RenderMode.Client },
  { path: 'espace-client', renderMode: RenderMode.Client },
  { path: 'espace-prestataire', renderMode: RenderMode.Client },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
