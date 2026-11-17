import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Profile } from '../../../../core/services/profile';


@Component({
imports: [FormsModule],
  selector: 'app-account',
  styleUrl: './account.css',
  templateUrl: './account.html',
})
export class Account {
  constructor(public profile: Profile) {
    this.updateInitials();
  }
  firstName: string = 'Camille';
  lastName: string = 'Dupont';
  email: string = 'camille@example.com';
  phone: string = '';
  selectedImage: string | null = null;

  onImageSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];

  if (file) {
    const imageUrl = URL.createObjectURL(file);
    this.profile.setProfileImage(imageUrl);
  }
}

  saveProfile() {
    console.log('Profile enregistré !');
    console.log('Prénom:', this.firstName);
    console.log('Nom:', this.lastName);
    console.log('Email:', this.email);
    console.log('Téléphone:', this.phone);
  }
  get initials(): string {
  return (this.firstName.charAt(0) + this.lastName.charAt(0)).toUpperCase();
}
updateInitials() {
  const initials = (
    this.firstName.charAt(0) + this.lastName.charAt(0)
  ).toUpperCase();

  this.profile.setProfileInitials(initials);
}
}