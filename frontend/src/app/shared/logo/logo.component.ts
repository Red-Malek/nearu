import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [RouterLink],
  template: `
    <a class="brand" [class.small]="size() === 'sm'" routerLink="/" aria-label="NearU, accueil">
      <span class="brand-mark" aria-hidden="true">⌂</span>
      <span class="brand-word">near<span>U</span></span>
    </a>`,
  styleUrl: './logo.component.scss',
})
export class LogoComponent {
  readonly size = input<'md' | 'sm'>('md');
}
