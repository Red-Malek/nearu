import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Logo officiel NearU (public/images).
 * tone="dark" : version au texte clair pour les fonds bleu nuit (écrans de connexion).
 */
@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [RouterLink],
  template: `
    <a class="brand" [class.small]="size() === 'sm'" routerLink="/" aria-label="NearU, accueil">
      <img [src]="src()" alt="NearU" width="293" height="83" />
    </a>`,
  styleUrl: './logo.component.scss',
})
export class LogoComponent {
  readonly size = input<'md' | 'sm'>('md');
  readonly tone = input<'light' | 'dark'>('light');
  readonly src = computed(() =>
    this.tone() === 'dark' ? '/images/nearu-logo-light.png' : '/images/nearu-logo.png',
  );
}
