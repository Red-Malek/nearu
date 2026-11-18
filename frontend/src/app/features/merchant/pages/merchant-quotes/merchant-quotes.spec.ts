import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MerchantQuotes } from './merchant-quotes';

describe('MerchantQuotes', () => {
  let component: MerchantQuotes;
  let fixture: ComponentFixture<MerchantQuotes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MerchantQuotes],
    }).compileComponents();

    fixture = TestBed.createComponent(MerchantQuotes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
