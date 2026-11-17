package com.nearu.backend.common.error;

import java.time.Instant;
import java.util.Map;

/**
 * Format JSON unique des erreurs renvoyées par l'API.
 * Le frontend affiche {@code message} et, pour un formulaire, {@code fieldErrors}.
 */
public record ApiError(int status, String message, Map<String, String> fieldErrors, Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Map.of(), Instant.now());
    }

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, message, fieldErrors, Instant.now());
    }
}
