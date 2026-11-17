package com.nearu.backend.profile;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain SQL, same approach as the search: read-only, only ACTIVE users are visible. */
@Repository
public class ProviderProfileRepository {

    private static final String PROFILE_SQL = """
            SELECT p.user_id,
                   p.display_name,
                   p.bio,
                   g.name AS city,
                   p.service_radius_km,
                   ARRAY(
                       SELECT c.name
                       FROM professional_services ps
                       JOIN service_categories c ON c.id = ps.category_id
                       WHERE ps.professional_id = p.user_id AND c.active
                       ORDER BY c.sort_order
                   ) AS services
            FROM professional_profiles p
            JOIN users u ON u.id = p.user_id AND u.status = 'ACTIVE'
            LEFT JOIN governorates g ON g.id = p.governorate_id
            WHERE p.user_id = :id
            """;

    private final JdbcClient jdbc;

    public ProviderProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProviderProfile> findActiveById(UUID id) {
        return jdbc.sql(PROFILE_SQL)
                .param("id", id)
                .query((rs, i) -> new ProviderProfile(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("bio"),
                        rs.getString("city"),
                        rs.getInt("service_radius_km"),
                        toList(rs.getArray("services"))))
                .optional();
    }

    private static List<String> toList(java.sql.Array array) throws SQLException {
        return List.of((String[]) array.getArray());
    }
}
