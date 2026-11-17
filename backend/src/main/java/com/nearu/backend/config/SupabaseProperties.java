package com.nearu.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration Supabase (section nearu.supabase de application.yml, alimentée par le fichier .env).
 *
 * @param url       URL du projet, ex. https://abcd.supabase.co
 * @param anonKey   clé publique « anon » / « publishable », transmise au frontend
 * @param jwksUrl   URL des clés publiques de signature (projets avec clés asymétriques)
 * @param jwtSecret ancien secret HS256 (projets « Legacy »), optionnel
 */
@ConfigurationProperties(prefix = "nearu.supabase")
public record SupabaseProperties(String url, String anonKey, String jwksUrl, String jwtSecret) {

    /** URL sans barre finale ni suffixe /auth/v1. */
    public String baseUrl() {
        String base = url == null ? "" : url.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/auth/v1")) {
            base = base.substring(0, base.length() - "/auth/v1".length());
        }
        return base;
    }

    /** Émetteur attendu dans le claim "iss" des jetons Supabase. */
    public String issuer() {
        return baseUrl() + "/auth/v1";
    }

    /** Message décrivant la configuration manquante, ou null si elle est complète. */
    public String missingConfiguration() {
        if (!StringUtils.hasText(url)) {
            return "SUPABASE_URL n'est pas défini dans le fichier .env";
        }
        if (!StringUtils.hasText(jwksUrl) && !StringUtils.hasText(jwtSecret)) {
            return "SUPABASE_JWKS_URL (ou SUPABASE_JWT_SECRET pour un projet « Legacy ») n'est pas défini dans le fichier .env";
        }
        return null;
    }
}
