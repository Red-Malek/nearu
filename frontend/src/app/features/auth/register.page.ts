import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, Role } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.page.html',
  styleUrl: './auth-form.scss',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly error = signal('');
  readonly loading = signal(false);

  readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['CUSTOMER' as Role, Validators.required],
  });

  async submit(): Promise<void> {
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    try {
      const me = await this.auth.signUp(this.form.getRawValue());
      await this.router.navigateByUrl(this.auth.spacePath(me.role));
    } catch (e) {
      this.error.set(e instanceof Error && e.message ? e.message : 'Une erreur est survenue. Veuillez réessayer.');
    } finally {
      this.loading.set(false);
    }
  }
}
