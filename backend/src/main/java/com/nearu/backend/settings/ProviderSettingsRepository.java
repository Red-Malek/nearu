package com.nearu.backend.settings;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain SQL on the existing tables only (no schema change). */
@Repository
public class ProviderSettingsRepository {

    public record UserRow(String role, String status, String fullName) {
    }

    private static final String FIND_SQL = """
            SELECT p.display_name, p.bio, p.address_line, p.governorate_id, p.service_radius_km,
                   p.accepting_requests, u.phone,
                   ARRAY(SELECT ps.category_id FROM professional_services ps
                         WHERE ps.professional_id = p.user_id ORDER BY ps.created_at) AS service_ids
            FROM professional_profiles p
            JOIN users u ON u.id = p.user_id
            WHERE p.user_id = :id
            """;

    private final JdbcClient jdbc;

    public ProviderSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findUser(UUID id) {
        return jdbc.sql("SELECT role, status, full_name FROM users WHERE id = :id")
                .param("id", id)
                .query((rs, i) -> new UserRow(rs.getString("role"), rs.getString("status"), rs.getString("full_name")))
                .optional();
    }

    public boolean profileExists(UUID id) {
        return jdbc.sql("SELECT count(*) FROM professional_profiles WHERE user_id = :id")
                .param("id", id).query(Integer.class).single() > 0;
    }

    public void createProfile(UUID id, String name) {
        jdbc.sql("INSERT INTO professional_profiles (user_id, display_name) VALUES (:id, :name)")
                .param("id", id).param("name", name).update();
    }

    public Optional<ProviderSettings> find(UUID id) {
        return jdbc.sql(FIND_SQL).param("id", id).query((rs, i) -> {
            UUID governorateId = rs.getObject("governorate_id", UUID.class);
            List<UUID> serviceIds = toList(rs.getArray("service_ids"));
            return new ProviderSettings(
                    rs.getString("display_name"), rs.getString("bio"), rs.getString("address_line"),
                    governorateId, rs.getInt("service_radius_km"), rs.getString("phone"), serviceIds,
                    rs.getBoolean("accepting_requests"), governorateId != null && !serviceIds.isEmpty());
        }).optional();
    }

    public int countActiveCategories(List<UUID> ids) {
        return jdbc.sql("SELECT count(*) FROM service_categories WHERE active AND id IN (:ids)")
                .param("ids", ids).query(Integer.class).single();
    }

    public boolean governorateExists(UUID id) {
        return jdbc.sql("SELECT count(*) FROM governorates WHERE id = :id")
                .param("id", id).query(Integer.class).single() > 0;
    }

    public void updateProfile(UUID id, String name, String bio, String address, UUID governorateId,
                              int radiusKm, boolean visible) {
        jdbc.sql("""
                UPDATE professional_profiles
                SET display_name = COALESCE(CAST(:name AS varchar), display_name),
                    bio = CAST(:bio AS text), address_line = CAST(:address AS varchar),
                    governorate_id = CAST(:gov AS uuid), service_radius_km = :radius,
                    accepting_requests = :visible, updated_at = now()
                WHERE user_id = :id
                """)
                .param("id", id).param("name", name).param("bio", bio).param("address", address)
                .param("gov", governorateId).param("radius", radiusKm).param("visible", visible).update();
    }

    public void updatePhone(UUID id, String phone) {
        jdbc.sql("UPDATE users SET phone = :phone, updated_at = now() WHERE id = :id")
                .param("id", id).param("phone", phone).update();
    }

    public void replaceServices(UUID id, List<UUID> categoryIds) {
        if (categoryIds.isEmpty()) {
            jdbc.sql("DELETE FROM professional_services WHERE professional_id = :id").param("id", id).update();
            return;
        }
        jdbc.sql("DELETE FROM professional_services WHERE professional_id = :id AND category_id NOT IN (:ids)")
                .param("id", id).param("ids", categoryIds).update();
        for (UUID categoryId : categoryIds) {
            jdbc.sql("INSERT INTO professional_services (professional_id, category_id) VALUES (:id, :cat) "
                    + "ON CONFLICT DO NOTHING").param("id", id).param("cat", categoryId).update();
        }
    }

    /** Soft delete: nothing is erased, the account just becomes DELETED (and leaves the search). */
    public void markDeleted(UUID id) {
        jdbc.sql("UPDATE professional_profiles SET accepting_requests = false, updated_at = now() WHERE user_id = :id")
                .param("id", id).update();
        jdbc.sql("UPDATE users SET status = 'DELETED', updated_at = now() WHERE id = :id").param("id", id).update();
    }

    private static List<UUID> toList(java.sql.Array array) throws SQLException {
        return List.of((UUID[]) array.getArray());
    }
}
