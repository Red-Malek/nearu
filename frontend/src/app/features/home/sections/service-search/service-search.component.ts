import { Component, afterNextRender, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Category, CategoryService } from '../../../../core/category.service';
import { Governorate, GovernorateService } from '../../../../core/governorate.service';

@Component({
  selector: 'app-service-search',
  standalone: true,
  imports: [],
  templateUrl: './service-search.component.html',
  styleUrl: './service-search.component.scss',
})
export class ServiceSearchComponent {
  private readonly categoryService = inject(CategoryService);
  private readonly governorateService = inject(GovernorateService);
  private readonly router = inject(Router);

  readonly categories = signal<Category[]>([]);
  readonly governorates = signal<Governorate[]>([]);
  readonly selectedCategory = signal('');
  readonly selectedCity = signal('');

  constructor() {
    // Browser only: the page is prerendered, there is no API at build time.
    afterNextRender(() => {
      this.categoryService.list().subscribe({
        next: (list) => this.categories.set(list),
        // On failure the dropdown keeps only "Tous les services".
        error: () => this.categories.set([]),
      });
      this.governorateService.list().subscribe({
        next: (list) => this.governorates.set(list),
        // On failure the dropdown keeps only "Toutes les villes".
        error: () => this.governorates.set([]),
      });
    });
  }

  onCategoryChange(event: Event): void {
    this.selectedCategory.set((event.target as HTMLSelectElement).value);
  }

  onCityChange(event: Event): void {
    this.selectedCity.set((event.target as HTMLSelectElement).value);
  }

  search(): void {
    this.router.navigate(['/prestataires'], {
      queryParams: { category: this.selectedCategory() || null, city: this.selectedCity() || null },
    });
  }
}
