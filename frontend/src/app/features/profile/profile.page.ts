import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, afterNextRender, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { ProviderProfile, ProviderProfileService } from '../../core/provider-profile.service';

type Status = 'loading' | 'ready' | 'not-found' | 'error';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss',
})
export class ProfilePage {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly profileService = inject(ProviderProfileService);

  readonly profile = signal<ProviderProfile | null>(null);
  readonly status = signal<Status>('loading');

  constructor() {
    // Browser only: the route is client-rendered, there is no API at build time.
    afterNextRender(() => {
      this.route.paramMap
        .pipe(
          tap(() => this.status.set('loading')),
          switchMap((params) =>
            this.profileService.get(params.get('id') ?? '').pipe(
              catchError((err: HttpErrorResponse) => {
                this.status.set(err.status === 404 ? 'not-found' : 'error');
                return EMPTY;
              }),
            ),
          ),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((profile) => {
          this.profile.set(profile);
          this.status.set('ready');
        });
    });
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  }
}
