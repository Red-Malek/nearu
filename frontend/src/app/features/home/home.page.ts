import { Component } from '@angular/core';
import { HeroComponent } from './sections/hero/hero.component';
import { ServiceSearchComponent } from './sections/service-search/service-search.component';
import { HowItWorksComponent } from './sections/how-it-works/how-it-works.component';
import { ProviderCtaComponent } from './sections/provider-cta/provider-cta.component';
import { AboutComponent } from './sections/about/about.component';
import { ContactComponent } from './sections/contact/contact.component';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    HeroComponent, ServiceSearchComponent,
    HowItWorksComponent, ProviderCtaComponent, AboutComponent, ContactComponent,
  ],
  template: `
    <app-hero />
    <app-service-search />
    <app-how-it-works />
    <app-provider-cta />
    <app-about />
    <app-contact />
  `,
})
export class HomePage {}
