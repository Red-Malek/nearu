import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Profile } from '../../../../core/services/profile';
import { CustomerSettingsService } from '../../../../core/customer-settings.service';
import { toApiError } from '../../../../core/api-error';

@Component({
  imports: [FormsModule],
  selector: 'app-account',
  styleUrl: './account.css',
  templateUrl: './account.html',
})
export class Account implements OnInit {
  private readonly settings = inject(CustomerSettingsService);
  private readonly cdr = inject(ChangeDetectorRef);

  firstName = '';
  lastName = '';
  email = '';
  phone = '';
  selectedImage: string | null = null;

  loading = true;
  saving = false;
  successMessage = '';
  errorMessage = '';

  constructor(public profile: Profile) {}

  ngOnInit(): void {
    this.settings.getProfile().subscribe({
      next: (p) => {
        this.firstName = p.firstName;
        this.lastName = p.lastName;
        this.email = p.email ?? '';
        this.phone = p.phone ?? '';
        this.loading = false;
        this.updateInitials();
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.errorMessage = toApiError(e).message;
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // La photo n'est pas encore enregistrée côté serveur : elle reste locale à la page.
  onImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];

    if (file) {
      const imageUrl = URL.createObjectURL(file);
      this.profile.setProfileImage(imageUrl);
    }
  }

  saveProfile() {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.firstName.trim() || !this.lastName.trim()) {
      this.errorMessage = 'Le prénom et le nom sont obligatoires.';
      return;
    }

    this.saving = true;
    this.settings
      .updateProfile({
        firstName: this.firstName,
        lastName: this.lastName,
        phone: this.phone.trim() || null,
      })
      .subscribe({
        next: (p) => {
          this.firstName = p.firstName;
          this.lastName = p.lastName;
          this.phone = p.phone ?? '';
          this.saving = false;
          this.successMessage = 'Modifications enregistrées.';
          this.updateInitials();
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.errorMessage = toApiError(e).message;
          this.saving = false;
          this.cdr.markForCheck();
        },
      });
  }

  get initials(): string {
    return (this.firstName.charAt(0) + this.lastName.charAt(0)).toUpperCase();
  }

  updateInitials() {
    this.profile.setProfileInitials(this.initials);
  }
}
