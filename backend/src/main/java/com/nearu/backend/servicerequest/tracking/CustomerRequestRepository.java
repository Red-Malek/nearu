package com.nearu.backend.servicerequest.tracking;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.nearu.backend.servicerequest.RequestTiming;
import com.nearu.backend.servicerequest.ServiceRequestStatus;
import com.nearu.backend.servicerequest.tracking.dto.MyRequestDetail;
import com.nearu.backend.servicerequest.tracking.dto.MyRequestSummary;

/**
 * Lectures et mises à jour SQL du suivi des demandes (US10) et de la décision sur le devis (US09).
 * Toutes les requêtes filtrent sur le client connecté : un client ne voit jamais la demande d'un autre.
 */
@Repository
public class CustomerRequestRepository {

    /** Montant affiché dans la liste : le devis actif (en attente ou accepté) d'abord, sinon le plus récent. */
    private static final String QUOTE_JOIN = """
            LEFT JOIN LATERAL (
                SELECT q.amount
                FROM quotes q
                WHERE q.request_id = r.id
                ORDER BY (q.status IN ('PENDING', 'ACCEPTED')) DESC, q.created_at DESC
                LIMIT 1
            ) q ON true
            """;

    private static final String SERVICE_NAME =
            "CASE WHEN c.slug = 'autre' THEN 'Autre : ' || r.category_other ELSE c.name END";

    private final JdbcClient jdbc;

    public CustomerRequestRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Devis tel qu'il est en base ; le service en déduit « expiré » et « décision possible ». */
    public record QuoteRow(UUID id, BigDecimal amount, String description, Instant proposedStart,
            int estimatedDurationMinutes, Instant validUntil, QuoteStatus status, Instant decidedAt,
            Instant createdAt) {
    }

    /** État du devis nécessaire à la décision. */
    public record QuoteDecisionRow(QuoteStatus status, Instant validUntil) {
    }

    public List<MyRequestSummary> findMine(UUID customerId) {
        return jdbc.sql("""
                SELECT r.id, r.title, %s AS service_name, r.professional_id, p.display_name,
                       r.status, q.amount AS quote_amount, r.created_at, r.updated_at
                FROM service_requests r
                JOIN service_categories c ON c.id = r.category_id
                JOIN professional_profiles p ON p.user_id = r.professional_id
                %s
                WHERE r.customer_id = :customerId
                ORDER BY r.updated_at DESC, r.created_at DESC
                """.formatted(SERVICE_NAME, QUOTE_JOIN))
            .param("customerId", customerId)
            .query((rs, rowNum) -> {
                ServiceRequestStatus status = ServiceRequestStatus.valueOf(rs.getString("status"));
                return new MyRequestSummary(rs.getObject("id", UUID.class), rs.getString("title"),
                        rs.getString("service_name"), rs.getObject("professional_id", UUID.class),
                        rs.getString("display_name"), status, status.getLabel(), rs.getBigDecimal("quote_amount"),
                        instant(rs, "created_at"), instant(rs, "updated_at"));
            })
            .list();
    }

    public Optional<MyRequestDetail> findMineDetail(UUID customerId, UUID requestId) {
        return jdbc.sql("""
                SELECT r.id, r.title, r.description, %s AS service_name, r.timing, r.preferred_date,
                       r.preferred_time, g.name AS governorate_name, r.address_line, r.address_note, r.status,
                       r.professional_id, p.display_name, r.created_at, r.updated_at
                FROM service_requests r
                JOIN service_categories c ON c.id = r.category_id
                JOIN professional_profiles p ON p.user_id = r.professional_id
                JOIN governorates g ON g.id = r.governorate_id
                WHERE r.id = :requestId AND r.customer_id = :customerId
                """.formatted(SERVICE_NAME))
            .param("requestId", requestId)
            .param("customerId", customerId)
            .query(CustomerRequestRepository::toDetail)
            .optional();
    }

    /** Tous les devis reçus pour une demande, du plus récent au plus ancien. */
    public List<QuoteRow> findQuotes(UUID requestId) {
        return jdbc.sql("""
                SELECT id, amount, description, proposed_start, estimated_duration_minutes, valid_until,
                       status, decided_at, created_at
                FROM quotes
                WHERE request_id = :requestId
                ORDER BY created_at DESC
                """)
            .param("requestId", requestId)
            .query((rs, rowNum) -> new QuoteRow(rs.getObject("id", UUID.class), rs.getBigDecimal("amount"),
                    rs.getString("description"), instant(rs, "proposed_start"),
                    rs.getInt("estimated_duration_minutes"), instant(rs, "valid_until"),
                    QuoteStatus.valueOf(rs.getString("status")), instant(rs, "decided_at"),
                    instant(rs, "created_at")))
            .list();
    }

    /** Verrouille la demande du client jusqu'à la fin de la transaction et renvoie son statut. */
    public Optional<ServiceRequestStatus> lockRequest(UUID customerId, UUID requestId) {
        return jdbc.sql("""
                SELECT status FROM service_requests
                WHERE id = :requestId AND customer_id = :customerId
                FOR UPDATE
                """)
            .param("requestId", requestId)
            .param("customerId", customerId)
            .query((rs, rowNum) -> ServiceRequestStatus.valueOf(rs.getString("status")))
            .optional();
    }

    /** Lit un devis de la demande (verrouillé) : un devis d'une autre demande n'est jamais renvoyé. */
    public Optional<QuoteDecisionRow> lockQuote(UUID requestId, UUID quoteId) {
        return jdbc.sql("""
                SELECT status, valid_until FROM quotes
                WHERE id = :quoteId AND request_id = :requestId
                FOR UPDATE
                """)
            .param("quoteId", quoteId)
            .param("requestId", requestId)
            .query((rs, rowNum) -> new QuoteDecisionRow(QuoteStatus.valueOf(rs.getString("status")),
                    instant(rs, "valid_until")))
            .optional();
    }

    /** Passe le devis de PENDING au statut décidé ; renvoie le nombre de lignes modifiées (0 ou 1). */
    public int decideQuote(UUID quoteId, QuoteStatus decision) {
        return jdbc.sql("""
                UPDATE quotes
                SET status = :decision, decided_at = now(), updated_at = now(), version = version + 1
                WHERE id = :quoteId AND status = 'PENDING'
                """)
            .param("decision", decision.name())
            .param("quoteId", quoteId)
            .update();
    }

    /** Change le statut de la demande seulement s'il vaut encore {@code from}. */
    public int changeRequestStatus(UUID requestId, ServiceRequestStatus from, ServiceRequestStatus to) {
        return jdbc.sql("""
                UPDATE service_requests
                SET status = :to, updated_at = now()
                WHERE id = :requestId AND status = :from
                """)
            .param("to", to.name())
            .param("from", from.name())
            .param("requestId", requestId)
            .update();
    }

    /** Trace la transition dans status_history (V1). */
    public void recordHistory(String entityType, UUID entityId, String from, String to, UUID actorId) {
        jdbc.sql("""
                INSERT INTO status_history (entity_type, entity_id, from_status, to_status, actor_id)
                VALUES (:entityType, :entityId, :from, :to, :actorId)
                """)
            .param("entityType", entityType)
            .param("entityId", entityId)
            .param("from", from)
            .param("to", to)
            .param("actorId", actorId)
            .update();
    }

    private static MyRequestDetail toDetail(ResultSet rs, int rowNum) throws SQLException {
        ServiceRequestStatus status = ServiceRequestStatus.valueOf(rs.getString("status"));
        RequestTiming timing = RequestTiming.valueOf(rs.getString("timing"));
        return new MyRequestDetail(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getString("description"), rs.getString("service_name"), timing, timing.getLabel(),
                rs.getObject("preferred_date", LocalDate.class), rs.getObject("preferred_time", LocalTime.class),
                rs.getString("governorate_name"), rs.getString("address_line"), rs.getString("address_note"), status,
                status.getLabel(), rs.getObject("professional_id", UUID.class), rs.getString("display_name"),
                instant(rs, "created_at"), instant(rs, "updated_at"), List.of());
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
