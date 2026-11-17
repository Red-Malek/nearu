import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { NavbarComponent } from './core/layout/navbar/navbar.component';
import { FooterComponent } from './core/layout/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);

  /** Vrai sur les écrans de connexion (route data.fullscreen) : ni navbar ni footer. */
  readonly fullscreen = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => App.deepestRoute(this.router.routerState.snapshot.root).data['fullscreen'] === true),
    ),
    { initialValue: false },
  );

  private static deepestRoute(route: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    return route.firstChild ? App.deepestRoute(route.firstChild) : route;
  }
}
