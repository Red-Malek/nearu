import { RouterLink } from '@angular/router';
import { Component, afterNextRender, inject, signal } from '@angular/core';
import { Category, CategoryService } from '../../../../core/category.service';

const ICONS: Record<string, string> = {
  plumbing: '🔧', electricity: '⚡', carpentry: '🪚', driver: '🚗', painting: '🎨',
  hvac: '❄️', repair: '🛠️', gardening: '🌿', cleaning: '🧹', masonry: '🧱',
};

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.scss',
})
export class CategoriesComponent {
  private readonly categoryService = inject(CategoryService);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly failed = signal(false);

  constructor() {
    // Browser only: the page is prerendered, there is no API at build time.
    afterNextRender(() => this.load());
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.categoryService.list().subscribe({
      next: (list) => { this.categories.set(list); this.loading.set(false); },
      error: () => { this.failed.set(true); this.loading.set(false); },
    });
  }

  icon(c: Category): string {
    return (c.icon && ICONS[c.icon]) || '🔎';
  }
}
