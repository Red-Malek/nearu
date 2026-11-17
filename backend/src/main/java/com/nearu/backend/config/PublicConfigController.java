package com.nearu.backend.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuration publique lue par le frontend au démarrage : l'URL Supabase et la clé
 * publique « anon ». Ainsi aucune clé n'est écrite dans le code Angular : tout vient du .env.
 * <p>
 * La clé anon est publique par conception (elle est visible dans tout navigateur) ;
 * la clé service_role et le secret JWT ne sont jamais exposés ici.
 */
@RestController
@RequestMapping("/api/public-config")
public class PublicConfigController {

    private final SupabaseProperties supabase;

    public PublicConfigController(SupabaseProperties supabase) {
        this.supabase = supabase;
    }

    @GetMapping
    public PublicConfigResponse get() {
        return new PublicConfigResponse(supabase.baseUrl(), supabase.anonKey() == null ? "" : supabase.anonKey().trim());
    }

    public record PublicConfigResponse(String supabaseUrl, String supabaseAnonKey) {
    }
}
