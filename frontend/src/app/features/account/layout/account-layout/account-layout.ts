import { Component } from '@angular/core';
import { RouterOutlet,RouterLink,RouterLinkActive} from '@angular/router';
import {
  LucideUserRound,
  LucideMapPin,
  LucideHeart,
  LucideHistory,
  LucideBell,
  LucideLockKeyhole
} from '@lucide/angular';
@Component({
  imports: [RouterOutlet,RouterLink,RouterLinkActive,
  LucideUserRound,
  LucideMapPin,
  LucideHeart,
  LucideHistory,
  LucideBell,
  LucideLockKeyhole],
  selector: 'app-account-layout',
  styleUrl: './account-layout.css',
  templateUrl: './account-layout.html',
})
export class AccountLayout {}