import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SearchPage } from './search.page';

describe('SearchPage', () => {
  let http: HttpTestingController;

  async function render(url = '/prestataires') {
    TestBed.configureTestingModule({
      imports: [SearchPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SearchPage);
    await fixture.whenStable();
    http.match('/api/categories').forEach((r) => r.flush([]));
    http.match('/api/governorates').forEach((r) => r.flush([]));
    return fixture;
  }

  it('shows name, métiers and ville of each result', async () => {
    const fixture = await render();
    http
      .expectOne((r) => r.url === '/api/providers')
      .flush([{ id: '1', name: 'Ahmed Trabelsi', city: 'Ariana', services: ['Plomberie', 'Électricité'] }]);
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ahmed Trabelsi');
    expect(text).toContain('Plomberie · Électricité');
    expect(text).toContain('Ariana');
    expect(text).toContain('1 résultat');
  });

  it('links each result to the public profile', async () => {
    const fixture = await render();
    http
      .expectOne((r) => r.url === '/api/providers')
      .flush([{ id: 'abc-123', name: 'Ahmed Trabelsi', city: 'Ariana', services: ['Électricité'] }]);
    await fixture.whenStable();

    const link = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('a.profile-link');
    expect(link?.textContent).toContain('Voir le profil');
    expect(link?.getAttribute('href')).toBe('/prestataires/abc-123');
  });

  it('shows an explicit message when nobody matches', async () => {
    const fixture = await render();
    http.expectOne((r) => r.url === '/api/providers').flush([]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucun prestataire ne correspond');
  });
});
