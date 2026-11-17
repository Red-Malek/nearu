import { Component, DestroyRef, afterNextRender, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { Category, CategoryService } from '../../core/category.service';
import { Governorate, GovernorateService } from '../../core/governorate.service';
import { ProviderSearchResult, ProviderSearchService } from '../../core/provider-search.service';

type Status = 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './search.page.html',
  styleUrl: './search.page.scss',
})
export class SearchPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly categoryService = inject(CategoryService);
  private readonly governorateService = inject(GovernorateService);
  private readonly providerSearch = inject(ProviderSearchService);

  readonly categories = signal<Category[]>([]);
  readonly governorates = signal<Governorate[]>([]);
  readonly category = signal('');
  readonly city = signal('');
  readonly providers = signal<ProviderSearchResult[]>([]);
  readonly status = signal<Status>('loading');

  constructor() {
    // Browser only: the route is client-rendered, there is no API at build time.
    afterNextRender(() => {
      this.categoryService.list().subscribe({ next: (l) => this.categories.set(l), error: () => {} });
      this.governorateService.list().subscribe({ next: (l) => this.governorates.set(l), error: () => {} });

      // The URL is the source of truth: /prestataires?category=electricite&city=ariana
      this.route.queryParamMap
        .pipe(
          tap((params) => {
            this.category.set(params.get('category') ?? '');
            this.city.set(params.get('city') ?? '');
            this.status.set('loading');
          }),
          switchMap((params) =>
            this.providerSearch.search(params.get('category'), params.get('city')).pipe(
              catchError(() => {
                this.status.set('error');
                return EMPTY;
              }),
            ),
          ),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((list) => {
          this.providers.set(list);
          this.status.set('ready');
        });
    });
  }

  onSubmit(event: Event, category: string, city: string): void {
    event.preventDefault();
    this.router.navigate(['/prestataires'], {
      queryParams: { category: category || null, city: city || null },
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
