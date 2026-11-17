import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { AuthService, Me } from './auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  const me = signal<Me | null>(null);
  const fakeAuth = {
    me,
    init: () => Promise.resolve(),
    spacePath: (role: string) => (role === 'CUSTOMER' ? '/espace-client' : '/espace-prestataire'),
  };

  function run(role: 'CUSTOMER' | 'PROFESSIONAL') {
    return TestBed.runInInjectionContext(() => roleGuard(role)({} as never, {} as never)) as Promise<
      boolean | UrlTree
    >;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: fakeAuth }],
    });
  });

  it('sends an anonymous visitor to the login page', async () => {
    me.set(null);
    const result = (await run('CUSTOMER')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/connexion');
  });

  it('lets the matching role in', async () => {
    me.set({ id: '1', fullName: 'Ahmed', role: 'PROFESSIONAL' });
    expect(await run('PROFESSIONAL')).toBe(true);
  });

  it('sends a customer away from the professional space, and the reverse', async () => {
    me.set({ id: '1', fullName: 'Sami', role: 'CUSTOMER' });
    const toCustomer = (await run('PROFESSIONAL')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(toCustomer)).toBe('/espace-client');

    me.set({ id: '2', fullName: 'Ahmed', role: 'PROFESSIONAL' });
    const toPro = (await run('CUSTOMER')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(toPro)).toBe('/espace-prestataire');
  });
});
