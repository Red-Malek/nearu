package com.nearu.backend.settings;

/** Profil modifiable du client connecté. L'e-mail est en lecture seule (géré par Supabase Auth). */
public record CustomerProfile(String firstName, String lastName, String email, String phone) {
}
