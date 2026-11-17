import { Component, DestroyRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';

/** Délai imposé par Supabase entre deux e-mails de réinitialisation pour une même adresse. */
const RESEND_DELAY_SECONDS = 60;

/** Écran « Mot de passe oublié ? » : envoie le lien de réinitialisation par e-mail. */
@Component({
  selector: 'app-forgot-password-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent],
  templateUrl: './forgot-password.page.html',
  styleUrls: ['../auth-form.scss'],
})
export class ForgotPasswordPage {
  private readonly auth = inject(AuthService);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** Adresse à laquelle le lien a été envoyé (affiche l'écran de confirmation). */
  readonly sentTo = signal<string | null>(null);
  readonly cooldown = signal(0);

  private timer: ReturnType<typeof setInterval> | null = null;

  readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.stopTimer());
  }

  get showEmailError(): boolean {
    const c = this.form.controls.email;
    return c.invalid && (c.touched || c.dirty);
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.cooldown() > 0) {
      return;
    }
    this.submitting.set(true);
    const email = this.form.controls.email.value.trim();
    const result = await this.auth.sendPasswordResetEmail(email);
    this.submitting.set(false);

    if (!result.ok) {
      this.errorMessage.set(result.message);
      return;
    }
    this.sentTo.set(email);
    this.startCooldown();
  }

  /** Revient au formulaire pour corriger l'adresse saisie. */
  changeEmail(): void {
    this.sentTo.set(null);
    this.errorMessage.set(null);
  }

  private startCooldown(): void {
    this.stopTimer();
    this.cooldown.set(RESEND_DELAY_SECONDS);
    this.timer = setInterval(() => {
      this.cooldown.update((value) => value - 1);
      if (this.cooldown() <= 0) {
        this.stopTimer();
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }
}
