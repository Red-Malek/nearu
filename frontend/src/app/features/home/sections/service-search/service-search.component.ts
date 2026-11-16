import { Component, afterNextRender, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Category, CategoryService } from '../../../../core/category.service';

@Component({
  selector: 'app-service-search',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './service-search.component.html',
  styleUrl: './service-search.component.scss',
})
export class ServiceSearchComponent {
  private readonly categoryService = inject(CategoryService);

  readonly categories = signal<Category[]>([]);
  readonly selectedCategory = signal('');

  constructor() {
    // Browser only: the page is prerendered, there is no API at build time.
    afterNextRender(() =>
      this.categoryService.list().subscribe({
        next: (list) => this.categories.set(list),
        // On failure the dropdown keeps only "Tous les services".
        error: () => this.categories.set([]),
      }),
    );
  }

  onCategoryChange(event: Event): void {
    this.selectedCategory.set((event.target as HTMLSelectElement).value);
  }
}
