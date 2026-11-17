import { Component, DestroyRef, OnInit, PLATFORM_ID, computed, inject, input, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { startWith } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { Governorate, GovernorateService } from '../../core/governorate.service';
import {
  REQUEST_LIMITS,
  RequestTarget,
  RequestTiming,
  ServiceRequest,
  ServiceRequestService,
} from '../../core/service-request.service';
import { LogoComponent } from '../../shared/logo/logo.component';

/** Valeur de l'option « Autre » dans la liste des services. */
const OTHER = 'autre';
const DRAFT_KEY = 'nearu.requestDraft.';

type Field =
  | 'categoryId'
  | 'otherCategory'
  | 'title'
  | 'description'
  | 'preferredDate'
  | 'governorateId'
  | 'addressLine'
  | 'addressNote';

/** Messages d'erreur de la maquette, champ par champ. */
const MESSAGES: Record<Field, string> = {
  categoryId: 'Choisissez le service concerné.',
  otherCategory: 'Précisez le service concerné (2 caractères minimum).',
  title: 'Donnez un titre court à votre demande.',
  description: 'Décrivez le problème en quelques mots (10 caractères minimum).',
  preferredDate: 'Choisissez une date.',
  governorateId: 'Choisissez votre gouvernorat.',
  addressLine: 'Indiquez l’adresse de l’intervention.',
  addressNote: 'Le repère ne doit pas dépasser 200 caractères.',
};

interface TimingChoice {
  value: RequestTiming;
  label: string;
  hint: string;
}

/** Date du jour au format AAAA-MM-JJ (fuseau local). */
function todayIso(): string {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

/**
 * Écran « Envoyer une demande » (US06). Le client arrive ici depuis le profil d'un prestataire
 * (/demandes/nouvelle?professionalId=…) : la demande part uniquement à ce prestataire, qui la
 * retrouve dans la section « Demandes » de son tableau de bord (GET /api/service-requests/received).
 */
@Component({
  selector: 'app-send-request-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LogoComponent],
  templateUrl: './send-request.page.html',
  styleUrls: ['./send-request.page.scss', './send-request.states.scss'],
})
export class SendRequestPage implements OnInit {
  private readonly api = inject(ServiceRequestService);
  private readonly governorateService = inject(GovernorateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly fb = inject(NonNullableFormBuilder);

  /** Prestataire choisi, transmis par le bouton « Envoyer une demande » de son profil. */
  readonly professionalId = input<string | undefined>();

  /** Un client connecté peut envoyer ; un visiteur est invité à se connecter (US06, critère 3). */
  readonly isLoggedIn = inject(AuthService).isLoggedIn;

  readonly limits = REQUEST_LIMITS;
  readonly other = OTHER;
  readonly today = todayIso();
  readonly timings: TimingChoice[] = [
    { value: 'URGENT', label: 'Urgent', hint: 'dans l’heure' },
    { value: 'TODAY', label: 'Aujourd’hui', hint: 'dans la journée' },
    { value: 'WITHIN_48H', label: 'Sous 48 h', hint: 'd’ici 2 jours' },
    { value: 'SCHEDULED', label: 'Choisir', hint: 'date et heure' },
  ];

  readonly target = signal<RequestTarget | null>(null);
  /** loading → chargement ; ready → formulaire ; missing → aucun prestataire choisi ; error → erreur. */
  readonly state = signal<'loading' | 'ready' | 'missing' | 'error'>('loading');
  readonly loadError = signal<string | null>(null);
  readonly governorates = signal<Governorate[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly created = signal<ServiceRequest | null>(null);
  readonly recap = signal<{ label: string; value: string }[]>([]);
  readonly position = signal<{ latitude: number; longitude: number } | null>(null);
  readonly geoHint = signal<{ text: string; tone: 'ok' | 'muted' | 'err' } | null>(null);

  readonly form = this.fb.group({
    categoryId: ['', Validators.required],
    otherCategory: [''],
    title: ['', [Validators.required, Validators.maxLength(REQUEST_LIMITS.titleMax)]],
    description: [
      '',
      [
        Validators.required,
        Validators.minLength(REQUEST_LIMITS.descriptionMin),
        Validators.maxLength(REQUEST_LIMITS.descriptionMax),
      ],
    ],
    timing: ['URGENT' as RequestTiming],
    preferredDate: [''],
    preferredTime: [''],
    governorateId: ['', Validators.required],
    addressLine: ['', [Validators.required, Validators.maxLength(REQUEST_LIMITS.addressMax)]],
    addressNote: ['', Validators.maxLength(REQUEST_LIMITS.addressNoteMax)],
  });

  private readonly descriptionValue = toSignal(this.form.controls.description.valueChanges, { initialValue: '' });
  readonly descriptionLength = computed(() => this.descriptionValue().length);
  private readonly timingValue = toSignal(this.form.controls.timing.valueChanges, { initialValue: 'URGENT' });
  readonly isScheduled = computed(() => this.timingValue() === 'SCHEDULED');
  private readonly categoryValue = toSignal(this.form.controls.categoryId.valueChanges, { initialValue: '' });
  readonly isOther = computed(() => this.categoryValue() === OTHER);

  readonly firstName = computed(() => this.target()?.displayName.split(/\s+/)[0] ?? '');
  readonly initials = computed(() =>
    (this.target()?.displayName ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word[0].toUpperCase())
      .join(''),
  );
  /** « Plombier · La Marsa, Tunis » dans la maquette : services et zone du prestataire. */
  readonly targetDetails = computed(() => {
    const target = this.target();
    if (!target) return '';
    const services = target.services.map((s) => s.name).join(', ') || 'Prestataire';
    return target.governorateName ? `${services} · ${target.governorateName}` : services;
  });

  ngOnInit(): void {
    this.watchConditionalFields();
    const id = this.professionalId();
    if (!id) {
      this.state.set('missing');
      return;
    }
    this.loadTarget(id);
    this.governorateService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => this.governorates.set(list),
        error: (error) => this.loadError.set(toApiError(error).message),
      });
  }

  private loadTarget(id: string): void {
    this.api
      .getTarget(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (target) => {
          this.target.set(target);
          this.state.set('ready');
          this.restoreDraft(target.id);
        },
        error: (error) => {
          this.loadError.set(toApiError(error).message);
          this.state.set('error');
        },
      });
  }

  /** « Précisez » obligatoire pour « Autre » ; date obligatoire pour « Choisir date et heure ». */
  private watchConditionalFields(): void {
    const c = this.form.controls;
    c.categoryId.valueChanges.pipe(startWith(c.categoryId.value), takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      c.otherCategory.setValidators(
        v === OTHER
          ? [
              Validators.required,
              Validators.minLength(REQUEST_LIMITS.otherCategoryMin),
              Validators.maxLength(REQUEST_LIMITS.otherCategoryMax),
            ]
          : [],
      );
      c.otherCategory.updateValueAndValidity({ emitEvent: false });
    });
    c.timing.valueChanges.pipe(startWith(c.timing.value), takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      c.preferredDate.setValidators(v === 'SCHEDULED' ? [Validators.required] : []);
      c.preferredDate.updateValueAndValidity({ emitEvent: false });
    });
  }

  /** Message sous un champ quand il est invalide et a été touché (ou après un envoi). */
  fieldError(field: Field): string | null {
    const control = this.form.controls[field];
    if (!control.invalid || !control.touched) return null;
    const server = control.errors?.['server'];
    return typeof server === 'string' ? server : MESSAGES[field];
  }

  /** « Utiliser ma position actuelle ». */
  useMyPosition(): void {
    if (!this.isBrowser || !navigator.geolocation) {
      this.geoHint.set({ text: 'La localisation n’est pas disponible sur cet appareil.', tone: 'err' });
      return;
    }
    this.geoHint.set({ text: 'Localisation en cours…', tone: 'muted' });
    navigator.geolocation.getCurrentPosition(
      (p) => {
        this.position.set({ latitude: p.coords.latitude, longitude: p.coords.longitude });
        this.geoHint.set({ text: 'Position enregistrée. Ajoutez le gouvernorat et l’adresse exacte.', tone: 'ok' });
      },
      () => {
        this.position.set(null);
        this.geoHint.set({ text: 'Position refusée. Saisissez votre adresse ci-dessous.', tone: 'err' });
      },
      { timeout: 8000 },
    );
  }

  submit(): void {
    this.errorMessage.set(null);
    const target = this.target();
    if (!target) return;
    if (!this.isLoggedIn()) {
      this.goToLogin();
      return;
    }
    const v = this.form.getRawValue();
    if (v.timing === 'SCHEDULED' && v.preferredDate && v.preferredDate < this.today) {
      this.form.controls.preferredDate.setErrors({ server: 'La date ne peut pas être dans le passé.' });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.focusFirstInvalid();
      return;
    }

    const isOther = v.categoryId === OTHER;
    const scheduled = v.timing === 'SCHEDULED';
    const position = this.position();
    this.submitting.set(true);
    this.api
      .create({
        professionalId: target.id,
        categoryId: isOther ? null : v.categoryId,
        otherCategory: isOther ? v.otherCategory.trim() : null,
        title: v.title.trim(),
        description: v.description.trim(),
        timing: v.timing,
        preferredDate: scheduled ? v.preferredDate : null,
        preferredTime: scheduled && v.preferredTime ? v.preferredTime : null,
        governorateId: v.governorateId,
        addressLine: v.addressLine.trim(),
        addressNote: v.addressNote.trim() || null,
        latitude: position?.latitude ?? null,
        longitude: position?.longitude ?? null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (request) => {
          this.submitting.set(false);
          this.recap.set(this.buildRecap(request, v.governorateId));
          this.created.set(request);
          if (this.isBrowser) window.scrollTo({ top: 0, behavior: 'smooth' });
        },
        error: (error) => {
          this.submitting.set(false);
          const apiError = toApiError(error);
          this.errorMessage.set(apiError.message);
          for (const [field, message] of Object.entries(apiError.fieldErrors)) {
            const control = this.form.get(field === 'professionalId' ? 'categoryId' : field);
            control?.setErrors({ server: message });
            control?.markAsTouched();
          }
        },
      });
  }

  /** Nouvelle demande au même prestataire. */
  startAgain(): void {
    this.created.set(null);
    this.errorMessage.set(null);
    this.position.set(null);
    this.geoHint.set(null);
    this.form.reset();
  }

  /** Garde la saisie pendant la connexion, puis revient sur ce formulaire. */
  goToLogin(): void {
    const target = this.target();
    if (this.isBrowser && target) {
      sessionStorage.setItem(DRAFT_KEY + target.id, JSON.stringify(this.form.getRawValue()));
    }
    void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  private restoreDraft(professionalId: string): void {
    if (!this.isBrowser) return;
    const saved = sessionStorage.getItem(DRAFT_KEY + professionalId);
    if (!saved) return;
    sessionStorage.removeItem(DRAFT_KEY + professionalId);
    try {
      this.form.patchValue(JSON.parse(saved));
    } catch {
      // brouillon illisible : on repart d'un formulaire vide
    }
  }

  private buildRecap(request: ServiceRequest, governorateId: string): { label: string; value: string }[] {
    const governorate = this.governorates().find((g) => g.id === governorateId)?.name ?? '';
    let delay = request.timingLabel;
    if (request.timing === 'SCHEDULED' && request.preferredDate) {
      const [y, m, d] = request.preferredDate.split('-');
      delay = `${d}/${m}/${y}` + (request.preferredTime ? ` à ${request.preferredTime.slice(0, 5)}` : '');
    }
    return [
      { label: 'Prestataire', value: request.professionalName },
      { label: 'Service', value: request.categoryName },
      { label: 'Demande', value: request.title },
      { label: 'Délai', value: delay },
      { label: 'Lieu', value: governorate ? `${request.addressLine}, ${governorate}` : request.addressLine },
      { label: 'Statut', value: request.statusLabel },
    ];
  }

  private focusFirstInvalid(): void {
    if (!this.isBrowser) return;
    setTimeout(() => document.querySelector<HTMLElement>('.request-page .bad input, .request-page .bad select, .request-page .bad textarea')?.focus());
  }
}
