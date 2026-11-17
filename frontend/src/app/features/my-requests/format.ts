/** Mises en forme françaises partagées par « Mes demandes » et son détail. */

const TUNIS = 'Africa/Tunis';

/** Montant en dinars avec 3 décimales (millimes), ex. « 120,500 TND ». */
export function formatTnd(amount: number | null | undefined): string {
  if (amount === null || amount === undefined) return '';
  return `${new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(amount)} TND`;
}

/** Date et heure, ex. « 18 novembre 2026 à 14:30 ». */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '';
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long', timeStyle: 'short', timeZone: TUNIS }).format(
    new Date(iso),
  );
}

/** Date seule à partir d'un horodatage, ex. « 18 nov. 2026 ». */
export function formatDay(iso: string | null | undefined): string {
  if (!iso) return '';
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeZone: TUNIS }).format(new Date(iso));
}

/** Date calendaire AAAA-MM-JJ, ex. « 22/11/2026 ». */
export function formatLocalDate(value: string | null | undefined): string {
  if (!value) return '';
  const [y, m, d] = value.split('-');
  return `${d}/${m}/${y}`;
}

/** Durée en minutes, ex. « 1 h 30 ». */
export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (!h) return `${m} min`;
  return m ? `${h} h ${String(m).padStart(2, '0')}` : `${h} h`;
}
