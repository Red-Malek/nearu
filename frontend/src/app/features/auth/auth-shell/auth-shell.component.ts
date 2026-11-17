import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LogoComponent } from '../../../shared/logo/logo.component';

/**
 * Mise en page commune aux écrans d'authentification (maquettes Connexion,
 * Mot de passe oublié, Créer un compte) : panneau bleu nuit à gauche, formulaire à droite.
 *
 * Le formulaire est projeté dans le contenu principal ; le lien en haut à droite
 * est projeté avec l'attribut `shellTopRight`.
 */
@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterLink, LogoComponent],
  templateUrl: './auth-shell.component.html',
  styleUrl: './auth-shell.component.scss',
})
export class AuthShellComponent {
  readonly titleStart = input.required<string>();
  readonly titleAccent = input.required<string>();
  readonly titleEnd = input('');
  readonly subtitle = input.required<string>();
  readonly items = input<string[]>([]);
  /** true : liste numérotée (étapes) ; false : liste à coches. */
  readonly numbered = input(false);
  readonly backLink = input('/');
  readonly backLabel = input('Retour à l’accueil');
}
