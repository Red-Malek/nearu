import { Component } from '@angular/core';
import { NavbarComponent } from './components/navbar/navbar.component';
import { HeroComponent } from './components/hero/hero.component';
import { ServiceSearchComponent } from './components/service-search/service-search.component';
import { HowItWorksComponent } from './components/how-it-works/how-it-works.component';
import { ProviderCtaComponent } from './components/provider-cta/provider-cta.component';
import { AboutComponent } from './components/about/about.component';
import { ContactComponent } from './components/contact/contact.component';
import { FooterComponent } from './components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    NavbarComponent, HeroComponent, ServiceSearchComponent,
    HowItWorksComponent, ProviderCtaComponent, AboutComponent,
    ContactComponent, FooterComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
