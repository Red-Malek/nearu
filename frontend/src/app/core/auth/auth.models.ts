export type UserRole = 'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN';

/** Réponse de GET /api/auth/me (CurrentUserResponse.java). */
export interface CurrentUser {
  id: string;
  fullName: string;
  role: UserRole;
}

/**
 * Page d'arrivée de chaque rôle après la connexion (US01, critère 2).
 * Le client reste sur l'accueil de la marketplace ; son espace est accessible depuis le menu du profil.
 */
export const ROLE_HOME: Record<UserRole, string> = {
  CUSTOMER: '/',
  PROFESSIONAL: '/mon-commerce/parametres',
  ADMIN: '/',
};

/** Longueur minimale du mot de passe (maquette « Créer un compte » : « Au moins 8 caractères »). */
export const PASSWORD_MIN_LENGTH = 8;
