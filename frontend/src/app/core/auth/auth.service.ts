import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { SupabaseClient } from '@supabase/supabase-js';
import { firstValueFrom } from 'rxjs';
import { toApiError } from '../api-error';
import { toAuthErrorMessage } from './auth-error';
import { CurrentUser, UserRole } from './auth.models';
import { SupabaseService } from './supabase.service';

/** Résultat d'une action d'authentification : succès, ou message d'erreur à afficher. */
export type AuthResult<T = void> = { ok: true; value: T } | { ok: false; message: string; code?: string };

/** Données du formulaire « Créer un compte ». */
export interface SignUpData {
  role: Extract<UserRole, 'CUSTOMER' | 'PROFESSIONAL'>;
  firstName: string;
  lastName: string;
  email: string;
  /** 8 chiffres, sans indicatif. */
  phone: string;
  password: string;
}

/**
 * Issue d'une inscription : connecté tout de suite (confirmation d'e-mail désactivée),
 * ou e-mail de vérification envoyé (confirmation activée dans Supabase).
 */
export type SignUpOutcome = { kind: 'signed-in'; user: CurrentUser } | { kind: 'confirm-email'; email: string };

/**
 * Point d'entrée unique de l'authentification côté Angular :
 * - inscription, connexion, vérification d'e-mail et mot de passe oublié via Supabase Auth ;
 * - profil et rôle de l'utilisateur via le backend (GET /api/auth/me, POST /api/auth/profile).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly supabase = inject(SupabaseService);
  private readonly http = inject(HttpClient);

  private readonly currentUserState = signal<CurrentUser | null>(null);
  readonly currentUser = this.currentUserState.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUserState() !== null);

  /**
   * Raison pour laquelle une session Supabase existante n'a pas pu être utilisée
   * (profil refusé, backend injoignable…), affichée sur la page de connexion.
   */
  private readonly sessionProblemState = signal<string | null>(null);
  readonly sessionProblem = this.sessionProblemState.asReadonly();

  private pendingUser: Promise<CurrentUser | null> | null = null;

  /** Problème de configuration Supabase (fichier .env), ou null. */
  get configError(): string | null {
    return this.supabase.configError;
  }

  /** Erreur contenue dans un lien reçu par e-mail (lien expiré…), ou null. */
  get linkError(): string | null {
    return this.supabase.urlAuthError;
  }

  /**
   * Jeton d'accès Supabase à envoyer au backend (rafraîchi automatiquement).
   * N'attend pas l'initialisation : l'intercepteur l'appelle aussi pour /api/public-config.
   */
  async getAccessToken(): Promise<string | null> {
    const client = this.supabase.client;
    if (!client) {
      return null;
    }
    const { data } = await client.auth.getSession();
    return data.session?.access_token ?? null;
  }

  /** Connexion e-mail + mot de passe, puis chargement (ou création) du profil applicatif. */
  async signIn(email: string, password: string, rememberMe: boolean): Promise<AuthResult<CurrentUser>> {
    const ready = await this.readyClient();
    if (!ready.ok) {
      return ready;
    }
    const client = ready.value;
    this.supabase.rememberMe = rememberMe;
    this.sessionProblemState.set(null);

    const { error } = await client.auth.signInWithPassword({ email: email.trim(), password });
    if (error) {
      return { ok: false, message: toAuthErrorMessage(error), code: error.code };
    }

    try {
      const user = await this.loadOrCreateProfile();
      this.currentUserState.set(user);
      return { ok: true, value: user };
    } catch (profileError) {
      await client.auth.signOut();
      this.currentUserState.set(null);
      return { ok: false, message: toApiError(profileError).message };
    }
  }

  /**
   * Inscription (US01) : crée le compte Supabase avec les informations du formulaire dans
   * ses métadonnées. Si la confirmation d'e-mail est activée, Supabase envoie l'e-mail de
   * vérification et le profil sera créé à la première connexion.
   */
  async signUp(data: SignUpData): Promise<AuthResult<SignUpOutcome>> {
    const ready = await this.readyClient();
    if (!ready.ok) {
      return ready;
    }
    const client = ready.value;
    this.supabase.rememberMe = true;
    this.sessionProblemState.set(null);
    const email = data.email.trim();

    const { data: result, error } = await client.auth.signUp({
      email,
      password: data.password,
      options: {
        emailRedirectTo: `${window.location.origin}/login`,
        data: {
          first_name: data.firstName.trim(),
          last_name: data.lastName.trim(),
          phone: data.phone.replace(/\s/g, ''),
          role: data.role,
        },
      },
    });
    if (error) {
      return { ok: false, message: toAuthErrorMessage(error), code: error.code };
    }
    // Confirmation activée : Supabase ne signale pas une adresse déjà inscrite par une
    // erreur, mais par un utilisateur sans « identities ».
    if (result.user && result.user.identities?.length === 0) {
      return { ok: false, message: 'Un compte existe déjà avec cette adresse e-mail.' };
    }
    if (!result.session) {
      return { ok: true, value: { kind: 'confirm-email', email } };
    }

    try {
      const user = await this.loadOrCreateProfile();
      this.currentUserState.set(user);
      return { ok: true, value: { kind: 'signed-in', user } };
    } catch (profileError) {
      await client.auth.signOut();
      return { ok: false, message: toApiError(profileError).message };
    }
  }

  /** Renvoie l'e-mail de vérification d'un compte pas encore confirmé. */
  async resendConfirmationEmail(email: string): Promise<AuthResult> {
    const ready = await this.readyClient();
    if (!ready.ok) {
      return ready;
    }
    const { error } = await ready.value.auth.resend({
      type: 'signup',
      email: email.trim(),
      options: { emailRedirectTo: `${window.location.origin}/login` },
    });
    return error ? { ok: false, message: toAuthErrorMessage(error), code: error.code } : { ok: true, value: undefined };
  }

  /**
   * Envoie l'e-mail de réinitialisation. Supabase répond de la même manière que le compte
   * existe ou non : on n'indique jamais à l'utilisateur si l'adresse est inscrite.
   */
  async sendPasswordResetEmail(email: string): Promise<AuthResult> {
    const ready = await this.readyClient();
    if (!ready.ok) {
      return ready;
    }
    const { error } = await ready.value.auth.resetPasswordForEmail(email.trim(), {
      redirectTo: `${window.location.origin}/reset-password`,
    });
    return error ? { ok: false, message: toAuthErrorMessage(error), code: error.code } : { ok: true, value: undefined };
  }

  /** Vrai si l'utilisateur est arrivé avec un lien de réinitialisation valide. */
  async hasRecoverySession(): Promise<boolean> {
    await this.supabase.init();
    if (this.supabase.urlAuthError) {
      return false;
    }
    return (await this.getAccessToken()) !== null;
  }

  async updatePassword(newPassword: string): Promise<AuthResult> {
    const ready = await this.readyClient();
    if (!ready.ok) {
      return ready;
    }
    const { error } = await ready.value.auth.updateUser({ password: newPassword });
    return error ? { ok: false, message: toAuthErrorMessage(error), code: error.code } : { ok: true, value: undefined };
  }

  async signOut(): Promise<void> {
    await this.supabase.client?.auth.signOut();
    this.currentUserState.set(null);
  }

  /**
   * Profil de l'utilisateur connecté, chargé une fois puis gardé en mémoire.
   * Renvoie null si personne n'est connecté ou si le profil est inaccessible ; dans ce
   * dernier cas la session est fermée et la raison est exposée par `sessionProblem`.
   */
  ensureCurrentUser(): Promise<CurrentUser | null> {
    const cached = this.currentUserState();
    if (cached) {
      return Promise.resolve(cached);
    }
    // Un seul chargement à la fois (navbar et guard peuvent le demander en même temps)
    this.pendingUser ??= this.loadCurrentUser().finally(() => (this.pendingUser = null));
    return this.pendingUser;
  }

  private async loadCurrentUser(): Promise<CurrentUser | null> {
    await this.supabase.init();
    if (!(await this.getAccessToken())) {
      return null;
    }
    try {
      const user = await this.loadOrCreateProfile();
      this.currentUserState.set(user);
      this.sessionProblemState.set(null);
      return user;
    } catch (error) {
      this.sessionProblemState.set(toApiError(error).message);
      await this.signOut();
      return null;
    }
  }

  /** Attend l'initialisation de Supabase et renvoie le client, ou la raison de l'échec. */
  private async readyClient(): Promise<AuthResult<SupabaseClient>> {
    await this.supabase.init();
    if (this.supabase.configError) {
      return { ok: false, message: this.supabase.configError };
    }
    const client = this.supabase.client;
    return client
      ? { ok: true, value: client }
      : { ok: false, message: 'Cette action n’est disponible que dans le navigateur.' };
  }

  /**
   * Charge le profil (GET /api/auth/me). S'il n'existe pas encore (première connexion
   * après la vérification de l'e-mail), il est créé à partir des données d'inscription.
   */
  private async loadOrCreateProfile(): Promise<CurrentUser> {
    try {
      return await firstValueFrom(this.http.get<CurrentUser>('/api/auth/me'));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        return firstValueFrom(this.http.post<CurrentUser>('/api/auth/profile', {}));
      }
      throw error;
    }
  }
}
