/** Thrown by AuthService with a message that is ready to show to the visitor (French). */
export class AuthFailure extends Error {}

/** Maps a Supabase Auth error code to a clear French message. */
export function frenchAuthMessage(code: string | undefined): string {
  switch (code) {
    case 'user_already_exists':
    case 'email_exists':
      return 'Cet email est déjà utilisé.';
    case 'email_address_invalid':
    case 'validation_failed':
      return 'Adresse email invalide.';
    case 'weak_password':
      return 'Mot de passe trop faible (8 caractères minimum).';
    case 'invalid_credentials':
      return 'Email ou mot de passe incorrect.';
    case 'over_request_rate_limit':
    case 'over_email_send_rate_limit':
      return 'Trop de tentatives. Réessayez dans quelques minutes.';
    default:
      return 'Une erreur est survenue. Veuillez réessayer.';
  }
}
