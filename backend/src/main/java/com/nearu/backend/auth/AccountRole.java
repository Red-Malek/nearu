package com.nearu.backend.auth;

/** Roles a visitor may choose at sign-up. ADMIN is deliberately absent: it can never be self-assigned. */
public enum AccountRole {
    CUSTOMER,
    PROFESSIONAL
}
