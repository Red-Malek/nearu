
import {
  Component,
  OnInit,
  inject,
  ChangeDetectorRef,
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  ServiceRequestService,
  QuoteResponse,
} from '../../../../core/service-request.service';

@Component({
  selector: 'app-merchant-quotes',
  imports: [CommonModule],
  templateUrl: './merchant-quotes.html',
  styleUrl: './merchant-quotes.scss',
})
export class MerchantQuotes implements OnInit {
  private readonly serviceRequestService = inject(ServiceRequestService);
  private readonly cdr = inject(ChangeDetectorRef);

  quotes: QuoteResponse[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.loadQuotes();
  }

  loadQuotes(): void {
    this.loading = true;
    this.error = '';

    this.serviceRequestService.getMyQuotes().subscribe({
      next: (data) => {
        console.log('Devis reçus :', data);

        this.quotes = data;
        this.loading = false;

        this.cdr.markForCheck();
      },

      error: (err) => {
        console.error('Erreur lors du chargement des devis :', err);

        this.error = 'Impossible de charger vos devis.';
        this.loading = false;

        this.cdr.markForCheck();
      },
    });
  }
}
