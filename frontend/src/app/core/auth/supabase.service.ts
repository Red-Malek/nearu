import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SupabaseClient, createClient } from '@supabase/supabase-js';
import { firstValueFrom } from 'rxjs';
import { toApiError } from '../api-error';

const REMEMBER_ME_KEY = 'nearu.rememberMe';

/** Réponse de GET /api/public-config (PublicConfigController.java). */
interface PublicConfig {
  supabaseUrl: string;
  supabaseAnonKey: string;
}

/**
 * Crée et partage l'unique client Supabase de l'application.
 * <p>
 * L'URL et la clé publique sont lues au démarrage depuis le backend (GET /api/public-config),
 * qui les prend dans le fichier .env : aucune clé n'est écrite dans le code Angular.
 * Navigateur uniquement : pendant le rendu serveur il n'y a ni session ni stockage.
 * <p>
 * « Se souvenir de moi » : session dans localStorage (conservée) ou sessionStorage
 * (supprimée à la fermeture de l'onglet).
 */
@Injectable({ providedIn: 'root' })
export class SupabaseService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly http = inject(HttpClient);

  private instance: SupabaseClient | null = null;
  private initPromise: Promise<void> | null = null;
  private configErrorValue: string | null = null;
  private initialUrlHash = '';

  /** Appelé au démarrage de l'application (app.config.ts), avant le routage. */
  init(): Promise<void> {
    if (!this.isBrowser) {
      return Promise.resolve();
    }
    this.initPromise ??= this.load();
    return this.initPromise;
  }

  /** Client Supabase, ou null (rendu serveur, configuration absente ou invalide). */
  get client(): SupabaseClient | null {
    return this.instance;
  }

  /** Raison pour laquelle Supabase n'est pas utilisable, ou null si tout va bien. */
  get configError(): string | null {
    return this.configErrorValue;
  }

  get rememberMe(): boolean {
    return this.isBrowser && localStorage.getItem(REMEMBER_ME_KEY) !== 'false';
  }

  set rememberMe(value: boolean) {
    if (this.isBrowser) {
      localStorage.setItem(REMEMBER_ME_KEY, String(value));
    }
  }

  /**
   * Erreur transmise par Supabase dans le lien reçu par e-mail (lien expiré, déjà utilisé…),
   * ou null. Exemple : #error=access_denied&error_code=otp_expired&error_description=...
   */
  get urlAuthError(): string | null {
    const params = new URLSearchParams(this.initialUrlHash.replace(/^#/, ''));
    const code = params.get('error_code') ?? params.get('error');
    if (!code) {
      return null;
    }
    if (code === 'otp_expired') {
      return 'Ce lien a expiré ou a déjà été utilisé. Demandez-en un nouveau.';
    }
    return `Lien invalide : ${params.get('error_description') ?? code}`;
  }

  private async load(): Promise<void> {
    // Supabase nettoie l'URL après l'avoir lue : on garde une copie pour les pages concernées.
    this.initialUrlHash = window.location.hash;

    let config: PublicConfig;
    try {
      config = await firstValueFrom(this.http.get<PublicConfig>('/api/public-config'));
    } catch (error) {
      this.fail(`Configuration Supabase indisponible : ${toApiError(error).message}`);
      return;
    }

    const problem = SupabaseService.checkConfig(config);
    if (problem) {
      this.fail(problem);
      return;
    }

    this.instance = createClient(config.supabaseUrl, config.supabaseAnonKey, {
      auth: {
        flowType: 'implicit',
        detectSessionInUrl: true,
        persistSession: true,
        autoRefreshToken: true,
        storage: {
          getItem: (key: string) => localStorage.getItem(key) ?? sessionStorage.getItem(key),
          setItem: (key: string, value: string) => {
            const target = this.rememberMe ? localStorage : sessionStorage;
            const other = this.rememberMe ? sessionStorage : localStorage;
            target.setItem(key, value);
            other.removeItem(key);
          },
          removeItem: (key: string) => {
            localStorage.removeItem(key);
            sessionStorage.removeItem(key);
          },
        },
      },
    });
  }

  private fail(message: string): void {
    this.configErrorValue = message;
    console.error('[NearU]', message);
  }

  private static checkConfig(config: PublicConfig): string | null {
    const url = config.supabaseUrl ?? '';
    const key = config.supabaseAnonKey ?? '';
    if (!/^https:\/\/[^/\s]+$/.test(url)) {
      return 'SUPABASE_URL est absent ou invalide dans le fichier .env (attendu : https://<projet>.supabase.co). Redémarrez le backend après correction.';
    }
    if (!key) {
      return 'SUPABASE_ANON_KEY est absent du fichier .env. Redémarrez le backend après correction.';
    }
    if (key.startsWith('sb_secret_') || SupabaseService.jwtRole(key) === 'service_role') {
      return 'SUPABASE_ANON_KEY contient une clé secrète : utilisez la clé « anon public » ou « publishable ».';
    }
    return null;
  }

  /** Rôle inscrit dans une clé Supabase au format JWT (« anon » ou « service_role »). */
  private static jwtRole(key: string): string | null {
    const payload = key.split('.')[1];
    if (!payload) {
      return null;
    }
    try {
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return (JSON.parse(json) as { role?: string }).role ?? null;
    } catch {
      return null;
    }
  }
}
