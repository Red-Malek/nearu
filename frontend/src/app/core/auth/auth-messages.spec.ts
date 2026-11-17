import { frenchAuthMessage } from './auth-messages';

describe('frenchAuthMessage', () => {
  it('explains a duplicate email and an invalid email clearly', () => {
    expect(frenchAuthMessage('user_already_exists')).toBe('Cet email est déjà utilisé.');
    expect(frenchAuthMessage('email_address_invalid')).toBe('Adresse email invalide.');
  });

  it('falls back to a generic message for unknown codes', () => {
    expect(frenchAuthMessage('something_else')).toContain('erreur');
    expect(frenchAuthMessage(undefined)).toContain('erreur');
  });
});
