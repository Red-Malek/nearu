
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ServiceRequestService,
  ReceivedRequest,
  CreateQuote
} from '../../../../core/service-request.service';

@Component({
  selector: 'app-merchant-requests',
  imports: [CommonModule, FormsModule],
  templateUrl: './merchant-requests.html',
  styleUrl: './merchant-requests.css',
})
export class MerchantRequests implements OnInit {

  private readonly cdr = inject(ChangeDetectorRef);
  private readonly requestService = inject(ServiceRequestService);

  requests: ReceivedRequest[] = [];
  loading = true;
  errorMessage = '';
  selectedRequestId: string | null = null;

quoteAmount: number | null = null;
proposedStart = '';
estimatedDurationMinutes: number | null = null;
validUntil = '';

quoteLoading = false;
quoteError = '';
quoteSuccess = '';

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.errorMessage = '';

    this.requestService.getReceivedRequests().subscribe({
      next: (data) => {
        this.requests = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Erreur lors du chargement :', error);
        this.errorMessage = 'Impossible de charger les demandes.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }
  openQuoteForm(requestId: string): void {
  this.selectedRequestId = requestId;

  this.quoteAmount = null;
  this.proposedStart = '';
  this.estimatedDurationMinutes = null;
  this.validUntil = '';

  this.quoteError = '';
  this.quoteSuccess = '';
}
submitQuote(): void {
  if (
    !this.selectedRequestId ||
    this.quoteAmount === null ||
    this.estimatedDurationMinutes === null ||
    !this.proposedStart ||
    !this.validUntil
  ) {
    this.quoteError = 'Veuillez remplir tous les champs.';
    return;
  }

  const startDate = new Date(this.proposedStart);
  const expirationDate = new Date(this.validUntil);

  if (startDate <= new Date()) {
    this.quoteError = 'La date de début doit être dans le futur.';
    return;
  }

  if (expirationDate <= startDate) {
    this.quoteError = 'La validité doit dépasser la date de début proposée.';
    return;
  }

  const quote: CreateQuote = {
    amount: this.quoteAmount,
    proposedStart: startDate.toISOString(),
    estimatedDurationMinutes: this.estimatedDurationMinutes,
    validUntil: expirationDate.toISOString()
  };

  this.quoteLoading = true;
  this.quoteError = '';

  this.requestService.createQuote(this.selectedRequestId, quote).subscribe({
    next: () => {
      this.quoteLoading = false;
      this.quoteSuccess = 'Votre devis a été envoyé avec succès.';
      this.selectedRequestId = null;
      this.cdr.markForCheck();
    },
    error: (error) => {
      console.error('Erreur lors de l’envoi du devis :', error);
      this.quoteLoading = false;
      this.quoteError = 'Impossible d’envoyer le devis.';
      this.cdr.markForCheck();
    }
  });
}
  rejectRequest(requestId: string): void {
  const confirmed = confirm('Voulez-vous vraiment refuser cette demande ?');

  if (!confirmed) {
    return;
  }

  this.requestService.rejectRequest(requestId).subscribe({
    next: () => {
      this.requests = this.requests.filter(
        request => request.id !== requestId
      );
      this.cdr.markForCheck();
    },
    error: (error) => {
      console.error('Erreur lors du refus :', error);
      this.errorMessage = 'Impossible de refuser cette demande.';
      this.cdr.markForCheck();
    }
  });
}
}
