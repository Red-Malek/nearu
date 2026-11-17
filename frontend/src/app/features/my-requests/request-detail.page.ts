import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { MyRequestDetail, MyRequestsService, statusTone } from '../../core/my-requests.service';
import { formatDateTime, formatDuration, formatLocalDate, formatTnd } from './format';

/**
 * Détail d'une demande (US10, critère 2 : les devis reçus s'affichent) et décision sur le devis (US09).
 * La décision demande une confirmation, car elle est définitive.
 */
@Component({
  selector: 'app-request-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './request-detail.page.html',
  styleUrls: ['./my-requests.scss', './request-detail.page.scss'],
})
export class RequestDetailPage implements OnInit {
  private readonly api = inject(MyRequestsService);

  /** Paramètre de route :id (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  readonly error = signal<string | null>(null);
  readonly request = signal<MyRequestDetail | null>(null);
  /** Décision en attente de confirmation. */
  readonly confirming = signal<{ quoteId: string; decision: 'accept' | 'reject' } | null>(null);
  readonly deciding = signal(false);
  readonly decisionError = signal<string | null>(null);
  readonly decisionDone = signal<string | null>(null);

  readonly tone = statusTone;
  readonly tnd = formatTnd;
  readonly dateTime = formatDateTime;
  readonly localDate = formatLocalDate;
  readonly duration = formatDuration;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.api.get(this.id()).subscribe({
      next: (r) => {
        this.request.set(r);
        this.state.set('ready');
      },
      error: (e) => {
        this.error.set(toApiError(e).message);
        this.state.set('error');
      },
    });
  }

  ask(quoteId: string, decision: 'accept' | 'reject'): void {
    this.decisionError.set(null);
    this.decisionDone.set(null);
    this.confirming.set({ quoteId, decision });
  }

  cancel(): void {
    this.confirming.set(null);
  }

  confirm(): void {
    const pending = this.confirming();
    if (!pending || this.deciding()) return;
    const { quoteId, decision } = pending;
    this.deciding.set(true);
    this.decisionError.set(null);
    const call: Observable<MyRequestDetail> =
      decision === 'accept'
        ? this.api.acceptQuote(this.id(), quoteId)
        : this.api.rejectQuote(this.id(), quoteId);
    call.subscribe({
      next: (r) => {
        this.request.set(r);
        this.deciding.set(false);
        this.confirming.set(null);
        this.decisionDone.set(
          decision === 'accept'
            ? `Devis accepté. ${r.professionalName} est informé de votre décision.`
            : `Devis refusé. ${r.professionalName} est informé de votre décision.`,
        );
      },
      error: (e) => {
        this.deciding.set(false);
        this.confirming.set(null);
        this.decisionError.set(toApiError(e).message);
        // Recharge : le devis a pu changer entre-temps (expiré, déjà traité…)
        this.api.get(this.id()).subscribe({ next: (r) => this.request.set(r) });
      },
    });
  }
}
