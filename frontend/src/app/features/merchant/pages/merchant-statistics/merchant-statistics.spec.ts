import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MerchantStatistics } from './merchant-statistics';

describe('MerchantStatistics', () => {
  let component: MerchantStatistics;
  let fixture: ComponentFixture<MerchantStatistics>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MerchantStatistics],
    }).compileComponents();

    fixture = TestBed.createComponent(MerchantStatistics);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
