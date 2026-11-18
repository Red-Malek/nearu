import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomerSettingsService } from '../../../../core/customer-settings.service';
import { toApiError } from '../../../../core/api-error';

@Component({
  imports: [FormsModule],
  selector: 'app-location',
  styleUrl: './location.css',
  templateUrl: './location.html',
})
export class Location implements OnInit {
  private readonly settings = inject(CustomerSettingsService);
  private readonly cdr = inject(ChangeDetectorRef);

  address = '';
  postalCode = '';
  city = '';
  searchRadius = 3;
  useGps = true;
  hideExactAddress = true;

  loading = true;
  saving = false;
  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.settings.getLocation().subscribe({
      next: (l) => {
        this.address = l.address ?? '';
        this.postalCode = l.postalCode ?? '';
        this.city = l.city ?? '';
        this.searchRadius = l.searchRadiusKm;
        this.useGps = l.useGps;
        this.hideExactAddress = l.hideExactAddress;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.errorMessage = toApiError(e).message;
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  saveLocation() {
    this.successMessage = '';
    this.errorMessage = '';
    this.saving = true;

    this.settings
      .updateLocation({
        address: this.address,
        postalCode: this.postalCode,
        city: this.city,
        searchRadiusKm: this.searchRadius,
        useGps: this.useGps,
        hideExactAddress: this.hideExactAddress,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.successMessage = 'Modifications enregistrées.';
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.errorMessage = toApiError(e).message;
          this.saving = false;
          this.cdr.markForCheck();
        },
      });
  }
}
