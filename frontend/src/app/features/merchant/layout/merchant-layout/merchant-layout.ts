import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-merchant-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './merchant-layout.html',
  styleUrl: './merchant-layout.css',
})
export class MerchantLayout {

}