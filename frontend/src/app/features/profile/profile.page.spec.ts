import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ProfilePage } from './profile.page';

describe('ProfilePage', () => {
  let http: HttpTestingController;

  async function render(id = 'abc-123') {
    TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ProfilePage);
    await fixture.whenStable();
    return fixture;
  }

  const text = (fixture: { nativeElement: unknown }) => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('shows name, métiers, zone d’intervention, description and the request button', async () => {
    const fixture = await render();
    http.expectOne('/api/providers/abc-123').flush({
      id: 'abc-123',
      name: 'Ahmed Trabelsi',
      bio: 'Installation électrique et dépannage.',
      city: 'Ariana',
      serviceRadiusKm: 15,
      services: ['Électricité', 'Plomberie'],
    });
    await fixture.whenStable();

    const content = text(fixture);
    expect(content).toContain('Ahmed Trabelsi');
    expect(content).toContain('Électricité · Plomberie');
    expect(content).toContain('Ariana et ses alentours, dans un rayon de 15 km');
    expect(content).toContain('Installation électrique et dépannage.');
    expect(content).toContain('Envoyer une demande');
  });

  it('shows a fallback text when the provider has no description', async () => {
    const fixture = await render();
    http.expectOne('/api/providers/abc-123').flush({
      id: 'abc-123',
      name: 'Sami Ben Ali',
      bio: null,
      city: null,
      serviceRadiusKm: 10,
      services: [],
    });
    await fixture.whenStable();

    expect(text(fixture)).toContain('pas encore ajouté de description');
  });

  it('shows a clear error page when the profile does not exist', async () => {
    const fixture = await render('inconnu');
    http.expectOne('/api/providers/inconnu').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    const content = text(fixture);
    expect(content).toContain('Ce profil n’existe pas');
    expect(content).not.toContain('Envoyer une demande');
  });
});
