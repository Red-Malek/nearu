package com.nearu.backend.auth;

import java.util.UUID;

/** The signed-in user as the app sees it: id (Supabase sub), name and role. */
public record Me(UUID id, String fullName, String role) {
}
