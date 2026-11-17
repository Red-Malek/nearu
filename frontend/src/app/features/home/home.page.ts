import { Component, inject } from '@angular/core';
import { HeroComponent } from './sections/hero/hero.component';
import { ServiceSearchComponent } from './sections/service-search/service-search.component';
import { HowItWorksComponent } from './sections/how-it-works/how-it-works.component';
import { ProviderCtaComponent } from './sections/provider-cta/provider-cta.component';
import { AboutComponent } from './sections/about/about.component';
import { ContactComponent } from './sections/contact/contact.component';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    HeroComponent, ServiceSearchComponent,
    HowItWorksComponent, ProviderCtaComponent, AboutComponent, ContactComponent,
  ],
  template: `
    <app-hero />
    @if (auth.currentUser()?.role !== 'PROFESSIONAL') {
      <app-service-search />
    }
    <app-how-it-works />
    @if (auth.currentUser()?.role !== 'PROFESSIONAL') {
      <app-provider-cta />
    }
    <app-about />
    <app-contact />
  `,
})
export class HomePage {
  readonly auth = inject(AuthService);
}
