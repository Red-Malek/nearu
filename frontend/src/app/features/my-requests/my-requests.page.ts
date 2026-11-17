import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toApiError } from '../../core/api-error';
import { MyRequestSummary, MyRequestsService, statusTone } from '../../core/my-requests.service';
import { formatDay, formatTnd } from './format';

/** « Mes demandes » (US10) : uniquement les demandes du client connecté, statut en toutes lettres. */
@Component({
  selector: 'app-my-requests-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-requests.page.html',
  styleUrls: ['./my-requests.scss', './my-requests.page.scss'],
})
export class MyRequestsPage implements OnInit {
  private readonly api = inject(MyRequestsService);

  readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  readonly error = signal<string | null>(null);
  readonly requests = signal<MyRequestSummary[]>([]);

  readonly tone = statusTone;
  readonly tnd = formatTnd;
  readonly day = formatDay;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.api.list().subscribe({
      next: (list) => {
        this.requests.set(list);
        this.state.set('ready');
      },
      error: (e) => {
        this.error.set(toApiError(e).message);
        this.state.set('error');
      },
    });
  }
}
