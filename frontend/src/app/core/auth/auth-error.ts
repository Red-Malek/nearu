/**
 * Traduit les erreurs de Supabase Auth en messages clairs pour l'utilisateur.
 * On s'appuie d'abord sur le `code` de l'erreur, puis sur son message et son statut HTTP
 * (les anciennes versions de Supabase ne renvoient pas toujours de code).
 * Le message d'origine n'est jamais masqué : s'il n'est pas reconnu, il est affiché tel quel
 * (et toujours journalisé dans la console du navigateur, F12).
 */
export function toAuthErrorMessage(error: unknown): string {
  const { code, status, message } = (error ?? {}) as { code?: string; status?: number; message?: string };
  const rawMessage = (message ?? '').toLowerCase();

  console.error('[Supabase Auth]', { code, status, message });

  const friendly = friendlyMessage(code, status, rawMessage);
  if (friendly) {
    return friendly;
  }
  return message
    ? `Supabase a refusé la demande (HTTP ${status ?? '?'}${code ? `, ${code}` : ''}) : ${message}`
    : `Supabase a refusé la demande (HTTP ${status ?? '?'}).`;
}

function friendlyMessage(code: string | undefined, status: number | undefined, raw: string): string | null {
  // Mauvaise configuration du frontend (URL ou clé Supabase)
  if (raw.includes('invalid api key') || raw.includes('no api key') || raw.includes('secret api key')) {
    return 'Clé Supabase refusée : vérifiez SUPABASE_ANON_KEY dans le fichier .env (clé « anon public » ou « publishable »), puis redémarrez le backend.';
  }
  if (code === 'invalid_credentials' || raw.includes('invalid login credentials')) {
    return 'E-mail ou mot de passe incorrect.';
  }
  if (code === 'email_not_confirmed' || raw.includes('email not confirmed')) {
    return 'Votre adresse e-mail n’est pas encore confirmée. Ouvrez le lien reçu par e-mail.';
  }
  if (code === 'user_already_exists' || code === 'email_exists' || raw.includes('already registered')) {
    return 'Un compte existe déjà avec cette adresse e-mail.';
  }
  if (code === 'email_address_not_authorized' || raw.includes('not authorized')) {
    return 'Supabase refuse d’envoyer un e-mail à cette adresse : le SMTP personnalisé n’est pas configuré (Supabase > Authentication > Emails > SMTP Settings).';
  }
  if (raw.includes('error sending') || code === 'unexpected_failure') {
    return 'Supabase n’a pas pu envoyer l’e-mail : vérifiez les réglages SMTP (Authentication > Emails > SMTP Settings) et les journaux Logs > Auth.';
  }

  switch (code) {
    case 'user_banned':
      return 'Ce compte a été suspendu. Contactez le support.';
    case 'over_request_rate_limit':
      return 'Trop de tentatives. Patientez quelques minutes avant de réessayer.';
    case 'over_email_send_rate_limit':
      return 'Limite d’envoi d’e-mails atteinte (Supabase > Authentication > Rate Limits). Patientez ou augmentez la limite après avoir configuré le SMTP.';
    case 'email_address_invalid':
    case 'validation_failed':
      return 'Cette adresse e-mail n’est pas valide.';
    case 'signup_disabled':
    case 'email_provider_disabled':
      return 'Les inscriptions par e-mail sont désactivées dans Supabase.';
    case 'weak_password':
      return 'Ce mot de passe est trop faible. Choisissez-en un plus long ou plus varié.';
    case 'same_password':
      return 'Le nouveau mot de passe doit être différent de l’ancien.';
    case 'session_not_found':
    case 'session_expired':
      return 'Votre session a expiré. Recommencez la procédure.';
  }

  if (status === 429) {
    return 'Trop de tentatives. Patientez quelques minutes avant de réessayer.';
  }
  if (status === 401 || status === 403) {
    return 'Supabase refuse la requête : vérifiez SUPABASE_URL et SUPABASE_ANON_KEY dans le fichier .env, puis redémarrez le backend.';
  }
  if (status === 0 || status === undefined) {
    return 'Impossible de joindre Supabase : vérifiez SUPABASE_URL dans le fichier .env (https://<projet>.supabase.co) et votre connexion Internet.';
  }
  return null;
}
