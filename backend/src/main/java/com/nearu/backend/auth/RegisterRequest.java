package com.nearu.backend.auth;

/** Body of POST /api/me, sent right after the Supabase sign-up. Email and password never reach our backend. */
public record RegisterRequest(String fullName, String role) {
}
