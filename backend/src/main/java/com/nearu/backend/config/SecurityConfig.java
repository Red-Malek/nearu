package com.nearu.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import com.nearu.backend.auth.SupabaseJwtAuthenticationConverter;
import com.nimbusds.jwt.JWTParser;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Sécurité de l'API. Les pages publiques (catégories, gouvernorats, recherche, profils)
 * restent accessibles sans compte ; les autres appels exigent un JWT Supabase
 * ({@code Authorization: Bearer <access_token>}). Aucune session côté serveur.
 */
@Configuration
@EnableConfigurationProperties(SupabaseProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Audience des jetons d'utilisateurs connectés chez Supabase. */
    private static final String SUPABASE_AUDIENCE = "authenticated";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SupabaseJwtAuthenticationConverter jwtConverter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error", "/actuator/health").permitAll()
                // Endpoints publics (US03, US04, US05 et configuration du frontend)
                .requestMatchers(HttpMethod.GET, "/api/public-config", "/api/categories", "/api/governorates",
                        "/api/providers", "/api/providers/**").permitAll()
                // US06 : le formulaire (prestataire choisi) est visible sans compte…
                .requestMatchers(HttpMethod.GET, "/api/service-requests/target/**").permitAll()
                // … mais seul un client connecté peut envoyer une demande
                .requestMatchers(HttpMethod.POST, "/api/service-requests").hasRole("CUSTOMER")
                // US09 / US10 : suivi des demandes et décision sur le devis, client uniquement
                .requestMatchers("/api/service-requests/mine", "/api/service-requests/mine/**").hasRole("CUSTOMER")
                // Demandes reçues : tableau de bord du prestataire
                .requestMatchers(HttpMethod.GET, "/api/service-requests/received").hasRole("PROFESSIONAL")
                .requestMatchers(HttpMethod.GET, "/api/service-requests/quotes").hasRole("PROFESSIONAL")
                .requestMatchers(HttpMethod.PATCH, "/api/service-requests/*/reject").hasRole("PROFESSIONAL")
                .requestMatchers(HttpMethod.POST, "/api/service-requests/*/quotes").hasRole("PROFESSIONAL")
                // US02 : paramètres du commerce du prestataire connecté
                .requestMatchers("/api/professional/**").hasRole("PROFESSIONAL")
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                .authenticationEntryPoint((request, response, ex) -> {
                    log.warn("Jeton refusé sur {} : {}", request.getRequestURI(), ex.getMessage());
                    writeError(response, HttpStatus.UNAUTHORIZED,
                            "Session non reconnue par le serveur : " + ex.getMessage());
                })
                .accessDeniedHandler((request, response, ex) -> writeError(response, HttpStatus.FORBIDDEN,
                        "Votre compte n'a pas accès à cette fonctionnalité.")))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> writeError(response, HttpStatus.UNAUTHORIZED,
                        "Vous devez être connecté pour effectuer cette action."))
                .accessDeniedHandler((request, response, e) -> writeError(response, HttpStatus.FORBIDDEN,
                        "Votre compte n'a pas accès à cette fonctionnalité.")));
        return http.build();
    }

    /**
     * Valide les JWT Supabase : signature, expiration, émetteur ({@code <SUPABASE_URL>/auth/v1})
     * et audience ({@code authenticated}).
     * <ul>
     * <li>ES256 / RS256 (projets récents) : clés publiques lues sur SUPABASE_JWKS_URL ;</li>
     * <li>HS256 (projets « Legacy ») : secret SUPABASE_JWT_SECRET.</li>
     * </ul>
     * Si la configuration est incomplète, l'application démarre quand même (les pages publiques
     * fonctionnent) mais chaque appel authentifié est refusé avec la raison exacte.
     */
    @Bean
    JwtDecoder jwtDecoder(SupabaseProperties properties) {
        String missing = properties.missingConfiguration();
        if (missing != null) {
            log.error("Configuration Supabase incomplète : {}. Les connexions seront refusées.", missing);
            return token -> {
                throw new BadJwtException("configuration Supabase incomplète côté serveur (" + missing + ")");
            };
        }

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null
                && jwt.getAudience().contains(SUPABASE_AUDIENCE)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult
                            .failure(new OAuth2Error("invalid_token", "audience du jeton invalide", null));
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), audienceValidator);

        NimbusJwtDecoder asymmetric = null;
        if (StringUtils.hasText(properties.jwksUrl())) {
            asymmetric = NimbusJwtDecoder.withJwkSetUri(properties.jwksUrl().trim())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
            asymmetric.setJwtValidator(validator);
        }
        NimbusJwtDecoder hmac = null;
        if (StringUtils.hasText(properties.jwtSecret())) {
            SecretKeySpec key = new SecretKeySpec(properties.jwtSecret().trim().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            hmac = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
            hmac.setJwtValidator(validator);
        }
        log.info("Validation des jetons Supabase : émetteur {}, JWKS {}, secret HS256 {}", properties.issuer(),
                asymmetric != null ? "oui" : "non", hmac != null ? "oui" : "non");

        NimbusJwtDecoder asymmetricDecoder = asymmetric;
        NimbusJwtDecoder hmacDecoder = hmac;
        return token -> {
            String algorithm;
            try {
                algorithm = JWTParser.parse(token).getHeader().getAlgorithm().getName();
            }
            catch (ParseException ex) {
                throw new BadJwtException("jeton illisible", ex);
            }
            if ("HS256".equals(algorithm)) {
                if (hmacDecoder == null) {
                    throw new BadJwtException(
                            "jeton HS256 (projet Supabase « Legacy ») mais SUPABASE_JWT_SECRET n'est pas défini");
                }
                return hmacDecoder.decode(token);
            }
            if (asymmetricDecoder == null) {
                throw new BadJwtException("jeton " + algorithm + " mais SUPABASE_JWKS_URL n'est pas défini");
            }
            try {
                return asymmetricDecoder.decode(token);
            }
            catch (BadJwtException ex) {
                throw ex;
            }
            catch (JwtException ex) {
                // Panne d'infrastructure (URL JWKS fausse, réseau…) : on la signale clairement
                log.error("Impossible de vérifier un jeton avec SUPABASE_JWKS_URL={} : {}", properties.jwksUrl(),
                        ex.getMessage());
                throw new JwtException("impossible de récupérer les clés Supabase depuis SUPABASE_JWKS_URL ("
                        + ex.getMessage() + ")", ex);
            }
        };
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
            .write("{\"status\":" + status.value() + ",\"message\":\"" + jsonEscape(message)
                    + "\",\"fieldErrors\":{}}");
    }

    private static String jsonEscape(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    }
                    else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
