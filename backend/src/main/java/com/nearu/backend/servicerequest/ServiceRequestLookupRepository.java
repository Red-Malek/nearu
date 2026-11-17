package com.nearu.backend.servicerequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.nearu.backend.servicerequest.dto.ReceivedRequestResponse;
import com.nearu.backend.servicerequest.dto.RequestTargetResponse;
import com.nearu.backend.servicerequest.dto.RequestTargetResponse.ServiceOption;

/**
 * Lectures SQL sur les tables des autres fonctionnalités (catégories, gouvernorats,
 * profils prestataires, utilisateurs), dans le même style que ProviderProfileRepository (US05).
 */
@Repository
public class ServiceRequestLookupRepository {

    /** Catégorie technique utilisée quand le client choisit « Autre » (migration V4, non listée). */
    public static final String OTHER_CATEGORY_SLUG = "autre";

    private final JdbcClient jdbc;

    public ServiceRequestLookupRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Prestataire actif et ses services, pour l'en-tête du formulaire. */
    public Optional<RequestTargetResponse> findRequestTarget(UUID professionalId) {
        return jdbc.sql("""
                SELECT p.user_id, p.display_name, p.accepting_requests, g.name AS governorate_name
                FROM professional_profiles p
                JOIN users u ON u.id = p.user_id AND u.status = 'ACTIVE' AND u.role = 'PROFESSIONAL'
                LEFT JOIN governorates g ON g.id = p.governorate_id
                WHERE p.user_id = :id
                """)
            .param("id", professionalId)
            .query((rs, rowNum) -> new RequestTargetResponse(rs.getObject("user_id", UUID.class),
                    rs.getString("display_name"), rs.getString("governorate_name"),
                    rs.getBoolean("accepting_requests"), List.of()))
            .optional()
            .map(target -> new RequestTargetResponse(target.id(), target.displayName(), target.governorateName(),
                    target.acceptingRequests(), findServices(professionalId)));
    }

    private List<ServiceOption> findServices(UUID professionalId) {
        return jdbc.sql("""
                SELECT c.id, c.name
                FROM professional_services ps
                JOIN service_categories c ON c.id = ps.category_id AND c.active
                WHERE ps.professional_id = :id
                ORDER BY c.sort_order, c.name
                """)
            .param("id", professionalId)
            .query((rs, rowNum) -> new ServiceOption(rs.getObject("id", UUID.class), rs.getString("name")))
            .list();
    }

    /** Nom d'une catégorie active proposée par ce prestataire. */
    public Optional<String> findOfferedCategoryName(UUID professionalId, UUID categoryId) {
        return jdbc.sql("""
                SELECT c.name
                FROM professional_services ps
                JOIN service_categories c ON c.id = ps.category_id AND c.active
                WHERE ps.professional_id = :professionalId AND c.id = :categoryId
                """)
            .param("professionalId", professionalId)
            .param("categoryId", categoryId)
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

    /** Demandes reçues par un prestataire, les plus récentes d'abord. */
    public List<ReceivedRequestResponse> findReceived(UUID professionalId) {
        return jdbc.sql("""
                SELECT r.id, cu.full_name AS customer_name, r.title, r.description,
                       CASE WHEN c.slug = 'autre' THEN 'Autre : ' || r.category_other ELSE c.name END AS service_name,
                       r.timing, r.urgency, r.preferred_date, r.preferred_time,
                       g.name AS governorate_name, r.address_line, r.address_note, r.lat, r.lng,
                       r.status, r.created_at
                FROM service_requests r
                JOIN users cu ON cu.id = r.customer_id
                JOIN service_categories c ON c.id = r.category_id
                JOIN governorates g ON g.id = r.governorate_id
                WHERE r.professional_id = :professionalId
                ORDER BY r.created_at DESC
                """)
            .param("professionalId", professionalId)
            .query(ServiceRequestLookupRepository::toReceived)
            .list();
    }

    private static ReceivedRequestResponse toReceived(ResultSet rs, int rowNum) throws SQLException {
        RequestTiming timing = RequestTiming.valueOf(rs.getString("timing"));
        ServiceRequestStatus status = ServiceRequestStatus.valueOf(rs.getString("status"));
        return new ReceivedRequestResponse(rs.getObject("id", UUID.class), rs.getString("customer_name"),
                rs.getString("title"), rs.getString("description"), rs.getString("service_name"), timing,
                timing.getLabel(), "URGENT".equals(rs.getString("urgency")),
                rs.getObject("preferred_date", LocalDate.class), rs.getObject("preferred_time", LocalTime.class),
                rs.getString("governorate_name"), rs.getString("address_line"), rs.getString("address_note"),
                rs.getObject("lat", Double.class), rs.getObject("lng", Double.class), status, status.getLabel(),
                rs.getTimestamp("created_at").toInstant());
    }
}
