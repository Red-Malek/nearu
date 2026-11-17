import { Component, HostListener, afterNextRender, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { LogoComponent } from '../../../shared/logo/logo.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, LogoComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly open = signal(false);
  readonly initials = computed(
    () =>
      (this.auth.currentUser()?.fullName ?? '')
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((w) => w[0])
        .join('')
        .toUpperCase() || 'NU',
  );

  constructor() {
    // Navigateur uniquement : affiche l'utilisateur connecté après un rechargement de page
    afterNextRender(() => void this.auth.ensureCurrentUser());
  }

  toggle(event: Event): void {
    event.stopPropagation();
    this.open.update((v) => !v);
  }

  close(): void {
    this.open.set(false);
  }

  /** Ferme la session Supabase et revient à l'accueil. */
  async logout(): Promise<void> {
    this.close();
    await this.auth.signOut();
    await this.router.navigateByUrl('/');
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.close();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }
}
