import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
@Component({
  imports: [FormsModule],
  selector: 'app-location',
  styleUrl: './location.css',
  templateUrl: './location.html',
})
export class Location {
  address:string='';
  postalCode:string='';
  city:string='';
  searchRadius:number=3;
  useGps:boolean=true;
  hideExactAddress:boolean=true;
  saveLocation() {
    console.log('Adresse :', this.address);
    console.log('Code postal :', this.postalCode);
    console.log('Ville :', this.city);
    console.log('Rayon :', this.searchRadius);
    console.log('GPS :', this.useGps);
    console.log('Masquer adresse :', this.hideExactAddress);
}
}
