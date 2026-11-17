import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MyRequestDetail, QuoteView } from '../../core/my-requests.service';
import { RequestDetailPage } from './request-detail.page';

describe('RequestDetailPage', () => {
  let http: HttpTestingController;

  const quote = (over: Partial<QuoteView> = {}): QuoteView => ({
    id: 'q-1',
    amount: 120.5,
    description: 'Diagnostic et réparation, pièces comprises.',
    proposedStart: '2026-11-20T09:00:00Z',
    estimatedDurationMinutes: 90,
    validUntil: '2026-11-25T09:00:00Z',
    status: 'PENDING',
    statusLabel: 'En attente de votre réponse',
    expired: false,
    canDecide: true,
    decidedAt: null,
    createdAt: '2026-11-17T09:00:00Z',
    ...over,
  });

  const detail = (over: Partial<MyRequestDetail> = {}): MyRequestDetail => ({
    id: 'r-1',
    title: 'Prises à installer',
    description: 'Trois prises au salon.',
    serviceName: 'Électricité',
    timing: 'TODAY',
    timingLabel: "Aujourd'hui",
    preferredDate: null,
    preferredTime: null,
    governorateName: 'Tunis',
    addressLine: '5 av. Bourguiba',
    addressNote: null,
    status: 'QUOTED',
    statusLabel: 'Devis reçu',
    professionalId: 'p-1',
    professionalName: 'Karim Jlassi',
    createdAt: '2026-11-17T08:00:00Z',
    updatedAt: '2026-11-17T09:00:00Z',
    quotes: [quote()],
    ...over,
  });

  async function render() {
    TestBed.configureTestingModule({
      imports: [RequestDetailPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(RequestDetailPage);
    fixture.componentRef.setInput('id', 'r-1');
    await fixture.whenStable();
    return fixture;
  }

  const el = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;
  const button = (fixture: { nativeElement: unknown }, label: string) =>
    Array.from(el(fixture).querySelectorAll('button')).find((b) => b.textContent?.trim() === label);

  afterEach(() => http.verify());

  it('shows the request, the professional, the status and every quote with price and description', async () => {
    const fixture = await render();
    http.expectOne('/api/service-requests/mine/r-1').flush(
      detail({
        quotes: [
          quote(),
          quote({ id: 'q-0', amount: 200, description: 'Ancien devis', status: 'WITHDRAWN', statusLabel: 'Retiré par le prestataire', canDecide: false }),
        ],
      }),
    );
    await fixture.whenStable();

    const content = el(fixture).textContent ?? '';
    expect(content).toContain('Prises à installer');
    expect(content).toContain('Karim Jlassi');
    expect(content).toContain('Statut : Devis reçu');
    expect(content).toContain('Devis reçus');
    expect(content).toContain('Diagnostic et réparation, pièces comprises.');
    expect(content).toContain('Ancien devis');
    expect(el(fixture).querySelectorAll('article.quote').length).toBe(2);
    expect(button(fixture, 'Accepter le devis')).toBeTruthy();
    expect(button(fixture, 'Refuser le devis')).toBeTruthy();
  });

  it('accepts the pending quote after confirmation and shows the new status', async () => {
    const fixture = await render();
    http.expectOne('/api/service-requests/mine/r-1').flush(detail());
    await fixture.whenStable();

    button(fixture, 'Accepter le devis')!.click();
    await fixture.whenStable();
    button(fixture, 'Oui, accepter')!.click();

    const req = http.expectOne('/api/service-requests/mine/r-1/quotes/q-1/accept');
    expect(req.request.method).toBe('POST');
    req.flush(
      detail({
        status: 'QUOTE_ACCEPTED',
        statusLabel: 'Devis accepté',
        quotes: [quote({ status: 'ACCEPTED', statusLabel: 'Devis accepté', canDecide: false, decidedAt: '2026-11-17T10:00:00Z' })],
      }),
    );
    await fixture.whenStable();

    const content = el(fixture).textContent ?? '';
    expect(content).toContain('Statut : Devis accepté');
    expect(button(fixture, 'Accepter le devis')).toBeUndefined();
    expect(button(fixture, 'Refuser le devis')).toBeUndefined();
  });

  it('shows the backend message when the decision is refused (already decided)', async () => {
    const fixture = await render();
    http.expectOne('/api/service-requests/mine/r-1').flush(detail());
    await fixture.whenStable();

    button(fixture, 'Refuser le devis')!.click();
    await fixture.whenStable();
    button(fixture, 'Oui, refuser')!.click();

    http
      .expectOne('/api/service-requests/mine/r-1/quotes/q-1/reject')
      .flush(
        { status: 409, message: 'Vous avez déjà répondu à ce devis : la décision est définitive.', fieldErrors: {} },
        { status: 409, statusText: 'Conflict' },
      );
    http.expectOne('/api/service-requests/mine/r-1').flush(
      detail({ status: 'QUOTE_REJECTED', statusLabel: 'Devis refusé', quotes: [quote({ status: 'REJECTED', statusLabel: 'Devis refusé', canDecide: false })] }),
    );
    await fixture.whenStable();

    const content = el(fixture).textContent ?? '';
    expect(content).toContain('la décision est définitive');
    expect(content).toContain('Statut : Devis refusé');
  });

  it('says when no quote has been received yet', async () => {
    const fixture = await render();
    http.expectOne('/api/service-requests/mine/r-1').flush(detail({ status: 'SUBMITTED', statusLabel: 'Envoyée', quotes: [] }));
    await fixture.whenStable();

    expect(el(fixture).textContent).toContain('Aucun devis reçu pour l’instant');
  });

  it('shows an error when the request belongs to someone else', async () => {
    const fixture = await render();
    http
      .expectOne('/api/service-requests/mine/r-1')
      .flush({ status: 404, message: 'Cette demande est introuvable.', fieldErrors: {} }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(el(fixture).textContent).toContain('Cette demande est introuvable.');
  });
});
