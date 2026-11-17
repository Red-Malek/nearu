import { HttpErrorResponse } from '@angular/common/http';

/** Format des erreurs renvoyées par le backend Spring (ApiError.java). */
export interface ApiError {
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
}

/**
 * Convertit n'importe quelle erreur HTTP en ApiError, en gardant toujours la vraie cause :
 * message du backend, message d'erreur Spring par défaut, ou description du problème réseau.
 */
export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: -1, message: 'Une erreur inattendue est survenue.', fieldErrors: {} };
  }
  if (error.status === 0) {
    return {
      status: 0,
      message: 'Impossible de joindre le serveur NearU : vérifiez que le backend est démarré (port 8080).',
      fieldErrors: {},
    };
  }

  const body = error.error as { message?: unknown; error?: unknown; fieldErrors?: Record<string, string> } | null;
  // Notre format (ApiError.java) ou celui de Spring Boot (/error avec include-message)
  if (body && typeof body === 'object' && typeof body.message === 'string' && body.message) {
    return { status: error.status, message: body.message, fieldErrors: body.fieldErrors ?? {} };
  }
  // Réponse HTML au lieu de JSON : l'appel n'a pas atteint le backend (proxy /api absent ?)
  if (typeof error.error === 'string' || error.status === 200) {
    return {
      status: error.status,
      message: `Réponse inattendue pour ${error.url ?? 'l’API'} (HTTP ${error.status}) : l’appel n’a pas atteint le backend. Lancez le frontend avec « npm start » (proxy /api).`,
      fieldErrors: {},
    };
  }
  const reason = body && typeof body.error === 'string' ? ` ${body.error}` : '';
  return {
    status: error.status,
    message: `Erreur du serveur (HTTP ${error.status}${reason}). Consultez le terminal du backend pour le détail.`,
    fieldErrors: {},
  };
}
