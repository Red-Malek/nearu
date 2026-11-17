import { RouterLink } from '@angular/router';
import { Component, inject } from '@angular/core';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({ selector:'app-hero', standalone: true,
  imports: [RouterLink], templateUrl:'./hero.component.html', styleUrl:'./hero.component.scss' })
export class HeroComponent {
  readonly auth = inject(AuthService);
}
