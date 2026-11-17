import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ROLE_HOME } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';

/** Écran « Connexion » (US01). */
@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent],
  templateUrl: './login.page.html',
  styleUrls: ['../auth-form.scss', './login.page.scss'],
})
export class LoginPage {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Paramètres d'URL liés automatiquement (withComponentInputBinding). */
  readonly returnUrl = input<string | undefined>();
  readonly reset = input<string | undefined>();

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Adresse dont l'e-mail n'est pas confirmé : propose de renvoyer l'e-mail de vérification. */
  readonly unconfirmedEmail = signal<string | null>(null);
  readonly resendState = signal<'idle' | 'sending' | 'sent'>('idle');

  readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [true],
  });

  showError(control: 'email' | 'password'): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    this.unconfirmedEmail.set(null);
    this.resendState.set('idle');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const { email, password, rememberMe } = this.form.getRawValue();
    const result = await this.auth.signIn(email, password, rememberMe);
    this.submitting.set(false);

    if (!result.ok) {
      this.errorMessage.set(result.message);
      if (result.code === 'email_not_confirmed') {
        this.unconfirmedEmail.set(email.trim());
      }
      this.form.controls.password.reset();
      return;
    }
    await this.router.navigateByUrl(this.safeReturnUrl() ?? ROLE_HOME[result.value.role]);
  }

  async resendConfirmation(): Promise<void> {
    const email = this.unconfirmedEmail();
    if (!email) {
      return;
    }
    this.resendState.set('sending');
    const result = await this.auth.resendConfirmationEmail(email);
    if (result.ok) {
      this.resendState.set('sent');
    } else {
      this.resendState.set('idle');
      this.errorMessage.set(result.message);
    }
  }

  /** N'accepte qu'un chemin interne, pour éviter une redirection vers un site externe. */
  private safeReturnUrl(): string | null {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : null;
  }
}
