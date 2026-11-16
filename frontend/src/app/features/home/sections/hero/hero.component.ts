import { RouterLink } from '@angular/router';
import { Component } from '@angular/core';
@Component({ selector:'app-hero', standalone: true,
  imports: [RouterLink], templateUrl:'./hero.component.html', styleUrl:'./hero.component.scss' })
export class HeroComponent {}
