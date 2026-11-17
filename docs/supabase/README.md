# Configuration Supabase de NearU (authentification et e-mails)

NearU utilise **Supabase Auth** pour les comptes : inscription, vérification de l'e-mail, connexion
et mot de passe oublié. Le backend Spring vérifie les jetons Supabase et gère les rôles dans la
table `users`. Les valeurs de configuration sont dans le fichier `.env` à la racine du dépôt :
le backend le lit au démarrage et transmet au frontend l'URL et la clé publique
(`GET /api/public-config`). **Aucune clé n'est écrite dans le code.**

```
Angular ──► Supabase Auth (inscription, connexion, e-mails)
   │
   └──► /api (proxy Angular) ──► Spring Boot ──► PostgreSQL
              Authorization: Bearer <jeton Supabase>
```

## 1. Fichier `.env` (racine du dépôt, jamais commité)

```bash
cp .env.example .env
```

| Variable | Où la trouver dans Supabase |
|---|---|
| `SUPABASE_URL` | Project Settings → API → **Project URL** (`https://<projet>.supabase.co`) |
| `SUPABASE_ANON_KEY` | Project Settings → API Keys → clé **anon public** (`eyJ…`) ou **publishable** (`sb_publishable_…`). Ne mettez **jamais** la clé `service_role` ou `sb_secret_…` |
| `SUPABASE_JWKS_URL` | `https://<projet>.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_JWT_SECRET` | Seulement si Project Settings → **JWT Keys** affiche « Legacy JWT secret » : copiez ce secret. Sinon laissez vide |

Après toute modification du `.env`, **redémarrez le backend**.

## 2. URL de redirection

Authentication → **URL Configuration** :

| Champ | Valeur (développement) |
|---|---|
| Site URL | `http://localhost:4200` |
| Redirect URLs | `http://localhost:4200/**` |

`/login` reçoit le lien de vérification d'inscription, et `/reset-password` le lien de
réinitialisation. En production, ajoutez aussi l'URL du site déployé (`https://<domaine>/**`).

## 3. Envoi des e-mails (SMTP) — obligatoire

Sans SMTP personnalisé, Supabase n'envoie des e-mails qu'aux **membres de l'équipe du projet**, et
seulement quelques-uns par heure. C'est la cause des erreurs « email address not authorized » et
« email rate limit exceeded ». Pour envoyer un e-mail à **chaque nouvel utilisateur**, configurez
un SMTP : Authentication → **Emails** → **SMTP Settings** → *Enable custom SMTP*.

**Option A : Gmail** (simple pour un projet étudiant, environ 500 e-mails par jour)

1. Sur le compte Google qui enverra les e-mails, activez la **validation en deux étapes**.
2. Compte Google → Sécurité → **Mots de passe des applications** : créez-en un, nommé « NearU ».
3. Dans Supabase :

| Champ | Valeur |
|---|---|
| Sender email | l'adresse Gmail |
| Sender name | `NearU` |
| Host | `smtp.gmail.com` |
| Port | `465` |
| Username | l'adresse Gmail |
| Password | le mot de passe d'application (16 caractères), **pas** le mot de passe Gmail |

**Option B : Brevo** (gratuit, 300 e-mails par jour, recommandé pour un site public) : créez un
compte, vérifiez l'adresse d'expédition, puis SMTP & API → clé SMTP. Host `smtp-relay.brevo.com`,
port `587`, avec l'identifiant et la clé SMTP fournis par Brevo.

Les identifiants SMTP restent **dans Supabase uniquement** : ils ne vont ni dans le `.env` ni
dans le code.

Ensuite, Authentication → **Rate Limits** : passez « Rate limit for sending emails » à une
valeur adaptée, par exemple 30 par heure (la limite par défaut est très basse).

## 4. Vérification du compte à l'inscription

Authentication → Sign In / Providers → **Email** :

* **Enable Email provider** : activé ;
* **Confirm email** : **activé**. Chaque nouvel utilisateur reçoit alors un e-mail de vérification
  et ne peut se connecter qu'après avoir cliqué sur le lien ;
* Minimum password length : `8`, comme le formulaire NearU.

Le parcours :

1. `/inscription` : Supabase crée le compte et envoie l'e-mail de vérification. Le prénom, le nom,
   le téléphone et le rôle sont enregistrés dans les métadonnées du compte.
2. L'utilisateur clique sur le lien et arrive sur `/login`, où il est connecté automatiquement.
   Le backend crée alors son profil dans `users` (`POST /api/auth/profile`).
3. Lien non reçu ou expiré : un bouton « Renvoyer l'e-mail de vérification » est proposé sur
   l'écran d'inscription et sur l'écran de connexion.

## 5. Modèles d'e-mails en français

Authentication → Emails → **Templates** : copiez le contenu des fichiers de
`docs/supabase/email-templates/` :

| Modèle Supabase | Fichier | Sujet conseillé |
|---|---|---|
| Confirm signup | `confirm-signup.html` | Confirmez votre compte NearU |
| Reset Password | `reset-password.html` | Réinitialisez votre mot de passe NearU |

## 6. Diagnostic

| Message affiché | Cause | Correction |
|---|---|---|
| « SUPABASE_URL est absent ou invalide… » | `.env` incomplet | Compléter le `.env` puis redémarrer le backend |
| « Configuration Supabase indisponible : impossible de joindre le serveur… » | Backend arrêté | Lancer le backend |
| « Clé Supabase refusée… » | Mauvaise clé dans `SUPABASE_ANON_KEY` | Recopier la clé anon / publishable |
| « Session non reconnue par le serveur : … HS256 … » | Projet « Legacy » | Renseigner `SUPABASE_JWT_SECRET` |
| « Session non reconnue par le serveur : … issuer … » | `SUPABASE_URL` ne correspond pas au projet | Corriger `SUPABASE_URL` |
| « Supabase refuse d'envoyer un e-mail à cette adresse… » | Pas de SMTP personnalisé | Section 3 |
| « Limite d'envoi d'e-mails atteinte… » | Rate limit | Section 3, Rate Limits |
| « Votre adresse e-mail n'est pas encore confirmée… » | Lien de vérification pas encore cliqué | Bouton « Renvoyer l'e-mail de vérification » |
| « Le prénom est obligatoire. » à la connexion | Compte créé à la main dans le tableau de bord Supabase, sans métadonnées | Créer les comptes via `/inscription` |

Chaque refus de jeton est aussi écrit dans le terminal du backend (`Jeton refusé sur …`), et
chaque erreur Supabase dans la console du navigateur (F12 → `[Supabase Auth]`).
