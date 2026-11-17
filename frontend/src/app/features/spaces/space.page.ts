import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

/** Placeholder landing page of each role. US02 (providers) and US06+ (customers) fill them in. */
@Component({
  selector: 'app-space-page',
  standalone: true,
  template: `
    <section class="section-wrap space">
      <h1>{{ title() }}</h1>
      <p>Bonjour {{ auth.me()?.fullName }}, bienvenue sur NearU.</p>
      <button type="button" (click)="logout()">Se déconnecter</button>
    </section>
  `,
  styles: `
    :host { display: block; padding: 56px 0 96px; }
    h1 { margin: 0 0 8px; color: #0b2343; }
    p { color: #3d4859; }
    button {
      margin-top: 16px; height: 44px; padding: 0 24px; border: 0; border-radius: 999px;
      background: #0b2343; color: #fff; font-family: inherit; font-weight: 700; cursor: pointer;
    }
  `,
})
export class SpacePage {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly title = computed(() =>
    this.auth.me()?.role === 'PROFESSIONAL' ? 'Espace prestataire' : 'Espace client',
  );

  async logout(): Promise<void> {
    await this.auth.signOut();
    await this.router.navigateByUrl('/');
  }
}
