import { RouterLink } from '@angular/router';
import { Component } from '@angular/core';
@Component({ selector:'app-about', standalone: true,
  imports: [RouterLink], templateUrl:'./about.component.html', styleUrl:'./about.component.scss' })
export class AboutComponent {}
