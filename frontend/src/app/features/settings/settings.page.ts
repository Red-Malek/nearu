import { Component, DestroyRef, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { firstValueFrom } from 'rxjs';
import { Category, CategoryService } from '../../core/category.service';
import { Governorate, GovernorateService } from '../../core/governorate.service';
import {
  ProviderSettings,
  ProviderSettingsService,
  ProviderSettingsUpdate,
} from '../../core/provider-settings.service';

type Tab = 'profil' | 'horaires' | 'adresse' | 'services' | 'contact' | 'notifications' | 'compte';
interface Day { name: string; short: string; on: boolean; from: string; to: string; }

/**
 * Paramètres du commerce (US02). Saved in the database: nom, description, adresse, ville, rayon,
 * téléphone, services, visibilité. The other controls are local for now (no column yet).
 */
@Component({
  selector: 'app-settings-page',
  imports: [FormsModule],
  templateUrl: './settings.page.html',
  styleUrls: ['./settings.page.scss', './settings.page.part2.scss'],
})
export class SettingsPage implements OnInit {
  private readonly api = inject(ProviderSettingsService);
  private readonly categories = inject(CategoryService);
  private readonly governorates = inject(GovernorateService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  authMsg = signal('');
  toast = signal('');
  statut = signal<'saved' | 'saving' | 'error'>('saved');
  now = signal(new Date());
  private loaded = false;
  private lastSaved = '';
  private timer: ReturnType<typeof setTimeout> | undefined;

  tab = signal<Tab>('profil');
  tabs: { id: Tab; label: string; icon: string }[] = [
    { id: 'profil', label: 'Profil du commerce', icon: 'M4 21V9l8-6 8 6v12M9 21v-6h6v6' },
    { id: 'horaires', label: 'Horaires', icon: 'M12 7v5l3 2M12 21a9 9 0 100-18 9 9 0 000 18z' },
    { id: 'adresse', label: 'Adresse et zone', icon: 'M12 21s7-6.2 7-11a7 7 0 10-14 0c0 4.8 7 11 7 11zM12 12a2 2 0 100-4 2 2 0 000 4z' },
    { id: 'services', label: 'Services proposés', icon: 'M14.7 6.3a4 4 0 00-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 005.4-5.4l-2.6 2.6-2.4-.6-.6-2.4z' },
    { id: 'contact', label: 'Contact', icon: 'M5 4h4l2 5-2.5 1.5a11 11 0 005 5L15 13l5 2v4a2 2 0 01-2 2A16 16 0 013 6a2 2 0 012-2z' },
    { id: 'notifications', label: 'Notifications', icon: 'M6 8a6 6 0 1112 0c0 7 3 8 3 8H3s3-1 3-8zM10 20a2 2 0 004 0' },
  ];
  accountIcon = 'M12 12a4 4 0 100-8 4 4 0 000 8zM4 21a8 8 0 0116 0';

  // Enregistrés en base
  nom = signal('');
  description = signal('');
  adresse = signal('');
  gouvernorat = signal('');
  rayon = signal('5 km');
  rayons = [1, 2, 5, 10, 20, 30, 50, 100].map((n) => n + ' km');
  tel = signal('');
  services = signal<string[]>([]);
  visible = signal(true);
  nouveau = signal('');
  // Locaux (pas encore de colonne en base)
  activite = signal('');
  jours = signal<Day[]>(
    ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'].map((n, i) => ({
      name: n, short: n.slice(0, 3), on: i < 6, from: '09:00', to: '19:00',
    })),
  );
  cp = signal('');
  masquer = signal(false);
  devis = signal(true);
  urgence = signal(false);
  email = signal('');
  site = signal('');
  recevoir = signal(true);
  notifEmail = signal(true);
  notifMobile = signal(true);

  catalogue = signal<Category[]>([]);
  gouvernorats = signal<Governorate[]>([]);
  activites = computed(() => this.catalogue());

  constructor() {
    // Enregistrement automatique 800 ms après la dernière modification
    effect(() => {
      const snapshot = JSON.stringify(this.payload());
      if (!this.loaded || snapshot === this.lastSaved) return;
      clearTimeout(this.timer);
      this.statut.set('saving');
      this.timer = setTimeout(() => this.enregistrer(snapshot), 800);
    });
    const clock = setInterval(() => this.now.set(new Date()), 60000);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(clock);
      clearTimeout(this.timer);
    });
  }

  nomService = (id: string) => this.catalogue().find((c) => c.id === id)?.name ?? '';
  nomGouvernorat = computed(() => this.gouvernorats().find((g) => g.id === this.gouvernorat())?.name ?? '');
  suggestions = computed(() => {
    const q = this.nouveau().trim().toLowerCase();
    return this.catalogue()
      .filter((c) => !this.services().includes(c.id) && c.name.toLowerCase().includes(q))
      .slice(0, 12);
  });

  previewServices = computed(() => this.services().slice(0, 3).map((id) => this.nomService(id)));
  previewHoraires = computed(() => {
    const open = this.jours().filter((j) => j.on);
    if (!open.length) return 'Fermé';
    const f = open[0], l = open[open.length - 1];
    const range = `${f.from.slice(0, 2)}h – ${l.to.slice(0, 2)}h`;
    return open.length === 1 ? `${f.short} : ${range}` : `${f.short} – ${l.short} : ${range}`;
  });
  previewAdresse = computed(() => {
    if (this.masquer()) return this.nomGouvernorat() || '—';
    const ville = [this.cp(), this.nomGouvernorat()].filter(Boolean).join(' ');
    return [this.adresse(), ville].filter(Boolean).join(', ') || '—';
  });
  ouvert = computed(() => {
    const d = this.now();
    const j = this.jours()[(d.getDay() + 6) % 7];
    const hm = d.toTimeString().slice(0, 5);
    return !!j && j.on && hm >= j.from && hm < j.to;
  });
  complet = computed(() => !!this.nom().trim() && this.services().length > 0 && !!this.gouvernorat());
  completion = computed(() => {
    const checks = [this.nom().trim(), this.description().trim(), this.services().length, this.gouvernorat(),
      this.adresse().trim(), this.tel().trim(), this.jours().some((j) => j.on)];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  });
  pill = computed(() =>
    !this.visible() ? 'Fiche en pause' : this.complet() ? 'Fiche visible dans les recherches' : 'Fiche incomplète, non visible');
  nonSauve = computed(() => ({
    profil: 'l’activité principale', horaires: 'les horaires', adresse: 'le code postal et le masquage de l’adresse',
    services: 'le devis gratuit et l’urgence', contact: 'l’e-mail, le site web et les messages',
    notifications: 'les notifications', compte: 'le changement de mot de passe',
  })[this.tab()]);

  updateDay(i: number, patch: Partial<Day>) {
    this.jours.update((js) => js.map((j, k) => (k === i ? { ...j, ...patch } : j)));
  }
  addService(id?: string) {
    if (!id) {
      const q = this.nouveau().trim().toLowerCase();
      const exact = this.catalogue().find((c) => c.name.toLowerCase() === q)?.id;
      const list = this.suggestions();
      id = exact ?? (q && list.length === 1 ? list[0].id : undefined);
      if (!id) { this.notify('Choisissez un service dans la liste proposée'); return; }
    }
    const sid = id;
    this.nouveau.set('');
    if (!this.services().includes(sid)) this.services.update((s) => [...s, sid]);
  }
  removeService(id: string) { this.services.update((l) => l.filter((x) => x !== id)); }

  async ngOnInit() {
    try {
      const [cats, gouvs] = await Promise.all([
        firstValueFrom(this.categories.list()),
        firstValueFrom(this.governorates.list()),
      ]);
      this.catalogue.set(cats);
      this.gouvernorats.set(gouvs);
      this.appliquer(await firstValueFrom(this.api.get()));
    } catch (e: any) {
      if (e?.status === 401 || e?.status === 403) {
        await this.router.navigateByUrl('/login');
        return;
      }
      this.authMsg.set('Impossible de charger votre fiche : serveur indisponible.');
    }
  }

  private appliquer(d: ProviderSettings) {
    this.nom.set(d.name);
    this.description.set(d.bio ?? '');
    this.adresse.set(d.address ?? '');
    this.gouvernorat.set(d.governorateId ?? '');
    this.rayon.set(d.serviceRadiusKm + ' km');
    this.tel.set(d.phone ?? '');
    this.services.set(d.serviceIds);
    this.visible.set(d.visible);
    this.lastSaved = JSON.stringify(this.payload());
    this.loaded = true;
  }

  private payload(): ProviderSettingsUpdate {
    return {
      name: this.nom().trim() ? this.nom() : null,
      bio: this.description(),
      address: this.adresse(),
      governorateId: this.gouvernorat() || null,
      serviceRadiusKm: parseInt(this.rayon(), 10),
      phone: this.tel(),
      serviceIds: this.services(),
      visible: this.visible(),
    };
  }

  private async enregistrer(snapshot: string) {
    try {
      await firstValueFrom(this.api.update(JSON.parse(snapshot)));
      this.lastSaved = snapshot;
      this.statut.set(JSON.stringify(this.payload()) === snapshot ? 'saved' : 'saving');
    } catch (e: any) {
      this.statut.set('error');
      this.notify(e?.status === 400 ? 'Modification refusée : un service et une ville sont obligatoires' : 'Échec de l’enregistrement');
    }
  }

  changerMotDePasse() { this.notify('Disponible avec la connexion (US01)'); }

  async supprimer() {
    if (!confirm('Supprimer définitivement votre fiche ? Cette action est irréversible.')) return;
    try {
      this.loaded = false;
      await firstValueFrom(this.api.remove());
      await this.auth.signOut();
      await this.router.navigateByUrl('/');
    } catch {
      this.loaded = true;
      this.notify('Suppression impossible');
    }
  }

  notify(msg: string) { this.toast.set(msg); setTimeout(() => this.toast.set(''), 3500); }
}
