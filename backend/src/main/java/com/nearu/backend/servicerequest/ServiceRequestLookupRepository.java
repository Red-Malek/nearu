package com.nearu.backend.servicerequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.nearu.backend.servicerequest.dto.EligibleProfessionalResponse;

/**
 * Lectures SQL sur les tables des autres fonctionnalités (catégories, gouvernorats,
 * profils prestataires), dans le même style que ProviderSearchRepository (US04).
 */
@Repository
public class ServiceRequestLookupRepository {

    /** Catégorie technique utilisée quand le client choisit « Autre » (migration V4, non listée). */
    public static final String OTHER_CATEGORY_SLUG = "autre";

    private static final int MAX_PROFESSIONALS = 50;

    private final JdbcClient jdbc;

    public ServiceRequestLookupRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Nom d'une catégorie proposée aux clients (active). */
    public Optional<String> findActiveCategoryName(UUID categoryId) {
        return jdbc.sql("SELECT name FROM service_categories WHERE id = :id AND active = true")
            .param("id", categoryId)
            .query(String.class)
            .optional();
    }

    /** Identifiant de la catégorie « Autre ». */
    public Optional<UUID> findOtherCategoryId() {
        return jdbc.sql("SELECT id FROM service_categories WHERE slug = :slug")
            .param("slug", OTHER_CATEGORY_SLUG)
            .query((rs, rowNum) -> rs.getObject("id", UUID.class))
            .optional();
    }

    public boolean governorateExists(UUID governorateId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM governorates WHERE id = :id)")
            .param("id", governorateId)
            .query(Boolean.class)
            .single();
    }

    /**
     * Prestataires actifs qui acceptent des demandes. Si une catégorie est donnée, seuls ceux
     * qui la proposent sont gardés (catégorie « Autre » : categoryId null, tous les prestataires).
     * Filtres optionnels : gouvernorat et nom affiché.
     */
    public List<EligibleProfessionalResponse> findEligibleProfessionals(UUID categoryId, UUID governorateId,
            String nameQuery) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.user_id, p.display_name, g.name AS governorate_name,
                       p.avg_rating, p.review_count, p.verification_status
                FROM professional_profiles p
                JOIN users u ON u.id = p.user_id
                LEFT JOIN governorates g ON g.id = p.governorate_id
                WHERE p.accepting_requests = true
                  AND u.status = 'ACTIVE'
                  AND u.role = 'PROFESSIONAL'
                """);
        Map<String, Object> params = new LinkedHashMap<>();
        if (categoryId != null) {
            sql.append("""
                      AND EXISTS (SELECT 1 FROM professional_services ps
                                  WHERE ps.professional_id = p.user_id AND ps.category_id = :categoryId)
                    """);
            params.put("categoryId", categoryId);
        }
        if (governorateId != null) {
            sql.append(" AND p.governorate_id = :governorateId");
            params.put("governorateId", governorateId);
        }
        if (nameQuery != null && !nameQuery.isBlank()) {
            sql.append(" AND p.display_name ILIKE :nameQuery");
            params.put("nameQuery", "%" + escapeLike(nameQuery.trim()) + "%");
        }
        sql.append(" ORDER BY p.avg_rating DESC, p.review_count DESC, p.display_name LIMIT ")
            .append(MAX_PROFESSIONALS);

        return jdbc.sql(sql.toString())
            .params(params)
            .query((rs, rowNum) -> new EligibleProfessionalResponse(rs.getObject("user_id", UUID.class),
                    rs.getString("display_name"), rs.getString("governorate_name"), rs.getBigDecimal("avg_rating"),
                    rs.getInt("review_count"), "VERIFIED".equals(rs.getString("verification_status"))))
            .list();
    }

    /**
     * Informations nécessaires pour vérifier qu'un prestataire peut recevoir la demande.
     * {@code offersCategory} vaut toujours vrai quand categoryId est null (catégorie « Autre »).
     */
    public Optional<ProfessionalTarget> findProfessionalTarget(UUID professionalId, UUID categoryId) {
        String offersCategory = categoryId == null
                ? "true"
                : "EXISTS (SELECT 1 FROM professional_services ps"
                        + " WHERE ps.professional_id = p.user_id AND ps.category_id = :categoryId)";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("professionalId", professionalId);
        if (categoryId != null) {
            params.put("categoryId", categoryId);
        }
        return jdbc.sql("""
                SELECT p.display_name,
                       p.accepting_requests,
                       (u.status = 'ACTIVE' AND u.role = 'PROFESSIONAL') AS active_professional,
                       %s AS offers_category
                FROM professional_profiles p
                JOIN users u ON u.id = p.user_id
                WHERE p.user_id = :professionalId
                """.formatted(offersCategory))
            .params(params)
            .query((rs, rowNum) -> new ProfessionalTarget(rs.getString("display_name"),
                    rs.getBoolean("accepting_requests"), rs.getBoolean("active_professional"),
                    rs.getBoolean("offers_category")))
            .optional();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record ProfessionalTarget(String displayName, boolean acceptingRequests, boolean activeProfessional,
            boolean offersCategory) {
    }
}
