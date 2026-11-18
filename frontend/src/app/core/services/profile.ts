import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class Profile {

    profileImage = signal<string | null>(null);
    profileInitials = signal<string>('CD');
  setProfileImage(image: string | null) {
    this.profileImage.set(image);
  }
    setProfileInitials(initials: string) {
    this.profileInitials.set(initials);
  }
}