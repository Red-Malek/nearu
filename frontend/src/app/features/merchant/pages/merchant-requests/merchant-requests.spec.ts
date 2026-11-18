import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MerchantRequests } from './merchant-requests';

describe('MerchantRequests', () => {
  let component: MerchantRequests;
  let fixture: ComponentFixture<MerchantRequests>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MerchantRequests],
    }).compileComponents();

    fixture = TestBed.createComponent(MerchantRequests);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
