import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthFailure } from '../../core/auth/auth-messages';
import { AuthService } from '../../core/auth/auth.service';
import { RegisterPage } from './register.page';

describe('RegisterPage', () => {
  // Plain fake rather than vi.fn(): a rejecting vi.fn() made this spec fail even though the page showed the message.
  const calls: unknown[] = [];
  let outcome: () => Promise<unknown> = () => Promise.resolve(undefined);
  const fakeAuth = {
    signUp: (input: unknown) => {
      calls.push(input);
      return outcome();
    },
    spacePath: () => '/espace-prestataire',
  };

  async function render() {
    TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: fakeAuth }],
    });
    const fixture = TestBed.createComponent(RegisterPage);
    await fixture.whenStable();
    return fixture;
  }

  function fill(root: HTMLElement, values: Record<string, string>) {
    for (const [id, value] of Object.entries(values)) {
      const input = root.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
  }

  beforeEach(() => {
    calls.length = 0;
    outcome = () => Promise.resolve(undefined);
  });

  it('does not call Supabase when the email is invalid', async () => {
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;
    fill(root, { fullName: 'Ahmed Trabelsi', email: 'pas-un-email', password: 'motdepasse1' });

    root.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(calls).toEqual([]);
    expect(root.textContent).toContain('adresse email valide');
  });

  it('shows the clear message when the email is already used', async () => {
    outcome = () => Promise.reject(new AuthFailure('Cet email est déjà utilisé.'));
    const fixture = await render();
    const root = fixture.nativeElement as HTMLElement;
    fill(root, { fullName: 'Ahmed Trabelsi', email: 'ahmed@example.com', password: 'motdepasse1' });

    root.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(root.textContent).toContain('Cet email est déjà utilisé.');
  });

  it('sends the chosen role and opens the space of that role', async () => {
    outcome = () => Promise.resolve({ id: '1', fullName: 'Ahmed Trabelsi', role: 'PROFESSIONAL' });
    const fixture = await render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const root = fixture.nativeElement as HTMLElement;
    fill(root, { fullName: 'Ahmed Trabelsi', email: 'ahmed@example.com', password: 'motdepasse1' });
    const professional = root.querySelector<HTMLInputElement>('input[value="PROFESSIONAL"]')!;
    professional.click();

    root.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(calls).toEqual([
      { fullName: 'Ahmed Trabelsi', email: 'ahmed@example.com', password: 'motdepasse1', role: 'PROFESSIONAL' },
    ]);
    expect(navigate).toHaveBeenCalledWith('/espace-prestataire');
  });
});
