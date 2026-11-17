import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { SupabaseClient, createClient } from '@supabase/supabase-js';
import { firstValueFrom } from 'rxjs';
import { AuthFailure, frenchAuthMessage } from './auth-messages';
import { SUPABASE } from './supabase.config';

export type Role = 'CUSTOMER' | 'PROFESSIONAL';

export interface Me {
  id: string;
  fullName: string;
  role: Role;
}

export interface SignUpInput {
  email: string;
  password: string;
  fullName: string;
  role: Role;
}

/** Supabase proves who the user is; our backend (GET/POST /api/me) says which role the user has. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private client: SupabaseClient | null = null;
  private initPromise: Promise<void> | null = null;

  readonly me = signal<Me | null>(null);

  // Browser only: never call these methods during server-side rendering.
  private supabase(): SupabaseClient {
    this.client ??= createClient(SUPABASE.url, SUPABASE.anonKey);
    return this.client;
  }

  spacePath(role: Role): string {
    return role === 'CUSTOMER' ? '/espace-client' : '/espace-prestataire';
  }

  async token(): Promise<string | null> {
    const { data } = await this.supabase().auth.getSession();
    return data.session?.access_token ?? null;
  }

  /** Restores the session after a page reload. Safe to call many times. */
  init(): Promise<void> {
    this.initPromise ??= this.restore();
    return this.initPromise;
  }

  private async restore(): Promise<void> {
    if (!(await this.token())) {
      return;
    }
    try {
      await this.loadMe();
    } catch {
      this.me.set(null);
    }
  }

  async signUp(input: SignUpInput): Promise<Me> {
    const { data, error } = await this.supabase().auth.signUp({
      email: input.email,
      password: input.password,
    });
    if (error) {
      throw new AuthFailure(frenchAuthMessage(error.code));
    }
    if (!data.session) {
      // "Confirm email" is still enabled in the Supabase project.
      throw new AuthFailure('Inscription impossible pour le moment. Contactez l’équipe NearU.');
    }
    const me = await firstValueFrom(
      this.http.post<Me>('/api/me', { fullName: input.fullName, role: input.role }),
    );
    this.me.set(me);
    this.initPromise = Promise.resolve();
    return me;
  }

  async signIn(email: string, password: string): Promise<Me> {
    const { error } = await this.supabase().auth.signInWithPassword({ email, password });
    if (error) {
      throw new AuthFailure(frenchAuthMessage(error.code));
    }
    try {
      const me = await this.loadMe();
      this.initPromise = Promise.resolve();
      return me;
    } catch {
      await this.supabase().auth.signOut();
      throw new AuthFailure('Compte incomplet ou suspendu. Contactez l’équipe NearU.');
    }
  }

  private async loadMe(): Promise<Me> {
    const me = await firstValueFrom(this.http.get<Me>('/api/me'));
    this.me.set(me);
    return me;
  }

  async signOut(): Promise<void> {
    await this.supabase().auth.signOut();
    this.me.set(null);
    this.initPromise = null;
  }
}
