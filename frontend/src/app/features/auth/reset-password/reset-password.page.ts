import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PASSWORD_MIN_LENGTH } from '../../../core/auth/auth.models';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShellComponent } from '../auth-shell/auth-shell.component';


function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
}

/**
 * Étape 3 du mot de passe oublié : page ouverte depuis le lien reçu par e-mail.
 * Supabase a déjà lu le jeton présent dans le lien ; on vérifie qu'il est valide
 * puis on enregistre le nouveau mot de passe.
 */
@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent],
  templateUrl: './reset-password.page.html',
  styleUrls: ['../auth-form.scss'],
})
export class ResetPasswordPage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly minLength = PASSWORD_MIN_LENGTH;
  /** 'checking' → vérification du lien ; 'ready' → formulaire ; 'invalid' → lien expiré. */
  readonly state = signal<'checking' | 'ready' | 'invalid'>('checking');
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = inject(NonNullableFormBuilder).group(
    {
      password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  async ngOnInit(): Promise<void> {
    this.state.set((await this.auth.hasRecoverySession()) ? 'ready' : 'invalid');
  }

  showError(control: 'password' | 'confirmPassword'): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  get showMismatch(): boolean {
    return this.form.hasError('mismatch') && this.form.controls.confirmPassword.touched;
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const result = await this.auth.updatePassword(this.form.controls.password.value);
    if (!result.ok) {
      this.submitting.set(false);
      this.errorMessage.set(result.message);
      return;
    }
    // Par sécurité, on ferme la session ouverte par le lien et on demande une nouvelle connexion.
    await this.auth.signOut();
    this.submitting.set(false);
    await this.router.navigate(['/login'], { queryParams: { reset: 'success' } });
  }
}
