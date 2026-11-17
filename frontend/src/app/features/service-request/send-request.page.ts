import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, debounceTime, distinctUntilChanged, forkJoin, map, of, startWith, switchMap, tap } from 'rxjs';
import { toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { Category, CategoryService } from '../../core/category.service';
import { Governorate, GovernorateService } from '../../core/governorate.service';
import {
  EligibleProfessional,
  REQUEST_LIMITS,
  ServiceRequest,
  ServiceRequestService,
} from '../../core/service-request.service';

/** Valeur de l'option « Autre » dans la liste des catégories. */
export const OTHER_CATEGORY = 'autre';

type RequestField =
  | 'categoryId'
  | 'otherCategory'
  | 'title'
  | 'description'
  | 'governorateId'
  | 'addressLine'
  | 'preferredDate'
  | 'professionalId';

/** Date du jour au format AAAA-MM-JJ (fuseau local), pour le champ « Date souhaitée ». */
function todayIso(): string {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

/**
 * Écran « Envoyer une demande » (US06). Visible par un visiteur, qui est invité à se connecter
 * pour l'envoyer ; l'envoi est réservé aux clients.
 */
@Component({
  selector: 'app-send-request-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './send-request.page.html',
  styleUrl: './send-request.page.scss',
})
export class SendRequestPage implements OnInit {
  private readonly api = inject(ServiceRequestService);
  private readonly categoryService = inject(CategoryService);
  private readonly governorateService = inject(GovernorateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);

  /** Vrai si un client est connecté ; sinon le visiteur est invité à se connecter (US06 critère 3). */
  readonly isLoggedIn = inject(AuthService).isLoggedIn;

  /** Pré-remplissage depuis l'URL, ex. bouton « Envoyer une demande » du profil public (US05). */
  readonly professionalId = input<string | undefined>();
  readonly categoryId = input<string | undefined>();

  readonly limits = REQUEST_LIMITS;
  readonly otherValue = OTHER_CATEGORY;
  readonly today = todayIso();

  readonly categories = signal<Category[]>([]);
  readonly governorates = signal<Governorate[]>([]);
  readonly optionsLoaded = signal(false);
  readonly optionsError = signal<string | null>(null);
  readonly professionals = signal<EligibleProfessional[]>([]);
  readonly professionalsLoading = signal(false);
  readonly professionalsError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly created = signal<ServiceRequest | null>(null);

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
    governorateId: ['', Validators.required],
    addressLine: ['', [Validators.required, Validators.maxLength(REQUEST_LIMITS.addressMax)]],
    preferredDate: [''],
    professionalId: ['', Validators.required],
  });

  readonly professionalSearch = this.fb.control('');

  private readonly descriptionValue = toSignal(this.form.controls.description.valueChanges, {
    initialValue: '',
  });
  readonly descriptionLength = computed(() => this.descriptionValue().length);

  private readonly categoryValue = toSignal(this.form.controls.categoryId.valueChanges, {
    initialValue: '',
  });
  readonly hasCategory = computed(() => this.categoryValue() !== '');
  readonly isOtherCategory = computed(() => this.categoryValue() === OTHER_CATEGORY);

  ngOnInit(): void {
    this.form.patchValue({
      categoryId: this.categoryId() ?? '',
      professionalId: this.professionalId() ?? '',
    });
    this.watchOtherCategory();
    this.loadOptions();
    this.watchProfessionals();
  }

  /** Catégories (US03) et gouvernorats (US04), via les services existants de l'équipe. */
  loadOptions(): void {
    this.optionsError.set(null);
    forkJoin({ categories: this.categoryService.list(), governorates: this.governorateService.list() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ categories, governorates }) => {
          this.categories.set(categories);
          this.governorates.set(governorates);
          this.optionsLoaded.set(true);
        },
        error: (error) => this.optionsError.set(toApiError(error).message),
      });
  }

  /** Le champ « Précisez votre catégorie » n'est obligatoire que pour « Autre ». */
  private watchOtherCategory(): void {
    const { categoryId, otherCategory } = this.form.controls;
    categoryId.valueChanges
      .pipe(startWith(categoryId.value), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        if (value === OTHER_CATEGORY) {
          otherCategory.setValidators([
            Validators.required,
            Validators.minLength(REQUEST_LIMITS.otherCategoryMin),
            Validators.maxLength(REQUEST_LIMITS.otherCategoryMax),
          ]);
        } else {
          otherCategory.clearValidators();
          otherCategory.setValue('', { emitEvent: false });
        }
        otherCategory.updateValueAndValidity({ emitEvent: false });
      });
  }

  /** Recharge les prestataires quand la catégorie, le gouvernorat ou la recherche change. */
  private watchProfessionals(): void {
    const controls = this.form.controls;
    combineLatest([
      controls.categoryId.valueChanges.pipe(startWith(controls.categoryId.value)),
      controls.governorateId.valueChanges.pipe(startWith(controls.governorateId.value)),
      this.professionalSearch.valueChanges.pipe(
        startWith(''),
        debounceTime(300),
        map((query) => query.trim()),
        distinctUntilChanged(),
      ),
    ])
      .pipe(
        tap(() => this.professionalsError.set(null)),
        switchMap(([categoryId, governorateId, query]) => {
          if (!categoryId) {
            return of({ categoryId, query, list: [] as EligibleProfessional[] });
          }
          this.professionalsLoading.set(true);
          const apiCategory = categoryId === OTHER_CATEGORY ? null : categoryId;
          return this.api.findEligibleProfessionals(apiCategory, governorateId || null, query).pipe(
            map((list) => ({ categoryId, query, list })),
            catchError((error) => {
              this.professionalsError.set(toApiError(error).message);
              return of({ categoryId, query, list: [] as EligibleProfessional[] });
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ categoryId, query, list }) => {
        this.professionalsLoading.set(false);
        this.professionals.set(list);
        // Un prestataire qui n'est plus éligible (autre catégorie / gouvernorat) est désélectionné.
        const selected = controls.professionalId.value;
        if (categoryId && selected && !query && !list.some((p) => p.id === selected)) {
          controls.professionalId.setValue('');
        }
      });
  }

  selectProfessional(id: string): void {
    this.form.controls.professionalId.setValue(id);
    this.form.controls.professionalId.markAsTouched();
  }

  /** Message d'erreur à afficher sous un champ, ou null. */
  fieldError(field: RequestField): string | null {
    const control = this.form.controls[field];
    if (!control.invalid || !(control.touched || control.dirty)) {
      return null;
    }
    const errors = control.errors ?? {};
    if (errors['server']) return errors['server'] as string;
    if (errors['required']) return this.requiredMessage(field);
    if (errors['minlength']) {
      return `Au moins ${(errors['minlength'] as { requiredLength: number }).requiredLength} caractères.`;
    }
    if (errors['maxlength']) {
      return `${(errors['maxlength'] as { requiredLength: number }).requiredLength} caractères maximum.`;
    }
    return 'Valeur invalide.';
  }

  /** Envoie le visiteur vers la connexion, puis le ramène sur ce formulaire. */
  goToLogin(): void {
    void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  submit(): void {
    this.errorMessage.set(null);
    if (!this.isLoggedIn()) {
      this.goToLogin();
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Certains champs sont manquants ou invalides.');
      return;
    }
    const value = this.form.getRawValue();
    if (value.preferredDate && value.preferredDate < this.today) {
      this.form.controls.preferredDate.setErrors({ server: 'La date ne peut pas être dans le passé.' });
      this.form.controls.preferredDate.markAsTouched();
      return;
    }

    const isOther = value.categoryId === OTHER_CATEGORY;
    this.submitting.set(true);
    this.api
      .create({
        categoryId: isOther ? null : value.categoryId,
        otherCategory: isOther ? value.otherCategory.trim() : null,
        title: value.title.trim(),
        description: value.description.trim(),
        governorateId: value.governorateId,
        addressLine: value.addressLine.trim(),
        preferredDate: value.preferredDate || null,
        professionalId: value.professionalId,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (request) => {
          this.submitting.set(false);
          this.created.set(request);
        },
        error: (error) => {
          this.submitting.set(false);
          const apiError = toApiError(error);
          this.errorMessage.set(apiError.message);
          for (const [field, message] of Object.entries(apiError.fieldErrors)) {
            const control = this.form.get(field);
            control?.setErrors({ server: message });
            control?.markAsTouched();
          }
        },
      });
  }

  /** Repart d'un formulaire vide pour une nouvelle demande. */
  startNew(): void {
    this.created.set(null);
    this.errorMessage.set(null);
    this.form.reset();
    this.professionalSearch.setValue('');
  }

  private requiredMessage(field: RequestField): string {
    switch (field) {
      case 'categoryId':
        return 'Choisissez une catégorie.';
      case 'otherCategory':
        return 'Précisez la catégorie de service souhaitée.';
      case 'governorateId':
        return 'Choisissez votre gouvernorat.';
      case 'professionalId':
        return 'Choisissez le prestataire à qui envoyer votre demande.';
      default:
        return 'Ce champ est obligatoire.';
    }
  }
}
