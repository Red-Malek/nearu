import { Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PASSWORD_MIN_LENGTH, ROLE_HOME } from '../../../core/auth/auth.models';
import { AuthService, SignUpData } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';

type RegisterField = 'firstName' | 'lastName' | 'email' | 'phone' | 'password' | 'acceptTerms';

/** Écran « Créer un compte » (US01) : client (Particulier) ou prestataire. */
@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent],
  templateUrl: './register.page.html',
  styleUrls: ['../auth-form.scss', './register.page.scss'],
})
export class RegisterPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** ?type=prestataire (lien « Inscrivez votre commerce ») présélectionne le compte prestataire. */
  readonly type = input<string | undefined>();

  readonly minLength = PASSWORD_MIN_LENGTH;
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Adresse en attente de confirmation (affiche l'écran « Vérifiez vos e-mails »). */
  readonly confirmEmail = signal<string | null>(null);
  readonly resendState = signal<'idle' | 'sending' | 'sent'>('idle');

  readonly form = inject(NonNullableFormBuilder).group({
    role: ['CUSTOMER' as SignUpData['role']],
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^\d{2}\s?\d{3}\s?\d{3}$/)]],
    password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]],
    acceptTerms: [false, Validators.requiredTrue],
  });

  ngOnInit(): void {
    if (this.type() === 'prestataire') {
      this.form.controls.role.setValue('PROFESSIONAL');
    }
  }

  setRole(role: SignUpData['role']): void {
    this.form.controls.role.setValue(role);
  }

  fieldError(field: RegisterField): string | null {
    const control = this.form.controls[field];
    if (!control.invalid || !(control.touched || control.dirty)) {
      return null;
    }
    switch (field) {
      case 'email':
        return control.hasError('required') ? 'L’e-mail est obligatoire.' : 'Adresse e-mail invalide.';
      case 'phone':
        return control.hasError('required')
          ? 'Le téléphone est obligatoire.'
          : 'Saisissez 8 chiffres, ex. 20 123 456.';
      case 'password':
        return control.hasError('required')
          ? 'Le mot de passe est obligatoire.'
          : `Au moins ${PASSWORD_MIN_LENGTH} caractères.`;
      case 'acceptTerms':
        return 'Vous devez accepter les conditions pour créer un compte.';
      default:
        return control.hasError('required') ? 'Ce champ est obligatoire.' : '60 caractères maximum.';
    }
  }

  /** Renvoie l'e-mail de vérification (lien non reçu ou expiré). */
  async resendConfirmation(): Promise<void> {
    const email = this.confirmEmail();
    if (!email) {
      return;
    }
    this.errorMessage.set(null);
    this.resendState.set('sending');
    const result = await this.auth.resendConfirmationEmail(email);
    if (result.ok) {
      this.resendState.set('sent');
    } else {
      this.resendState.set('idle');
      this.errorMessage.set(result.message);
    }
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const value = this.form.getRawValue();
    const result = await this.auth.signUp({
      role: value.role,
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      phone: value.phone,
      password: value.password,
    });
    this.submitting.set(false);

    if (!result.ok) {
      this.errorMessage.set(result.message);
      return;
    }
    if (result.value.kind === 'confirm-email') {
      this.confirmEmail.set(result.value.email);
      return;
    }
    await this.router.navigateByUrl(ROLE_HOME[result.value.user.role]);
  }
}
