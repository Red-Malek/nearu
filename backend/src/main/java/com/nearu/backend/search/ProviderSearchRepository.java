package com.nearu.backend.search;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain SQL on purpose: the provider entity belongs to US02, and search only needs a read-only view.
 * A null filter means "no filter on this criterion".
 */
@Repository
public class ProviderSearchRepository {

    private static final String SEARCH_SQL = """
            SELECT p.user_id,
                   p.display_name,
                   g.name AS city,
                   ARRAY(
                       SELECT c.name
                       FROM professional_services ps
                       JOIN service_categories c ON c.id = ps.category_id
                       WHERE ps.professional_id = p.user_id AND c.active
                       ORDER BY c.sort_order
                   ) AS services
            FROM professional_profiles p
            JOIN users u ON u.id = p.user_id AND u.status = 'ACTIVE'
            JOIN governorates g ON g.id = p.governorate_id
            WHERE (CAST(:city AS VARCHAR) IS NULL OR g.code = :city)
              AND (CAST(:category AS VARCHAR) IS NULL OR EXISTS (
                       SELECT 1
                       FROM professional_services ps
                       JOIN service_categories c ON c.id = ps.category_id
                       WHERE ps.professional_id = p.user_id AND c.active AND c.slug = :category))
            ORDER BY p.display_name
            """;

    private final JdbcClient jdbc;

    public ProviderSearchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProviderSearchResult> search(String categorySlug, String cityCode) {
        return jdbc.sql(SEARCH_SQL)
                .param("category", categorySlug)
                .param("city", cityCode)
                .query((rs, i) -> new ProviderSearchResult(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("city"),
                        toList(rs.getArray("services"))))
                .list();
    }

    private static List<String> toList(java.sql.Array array) throws SQLException {
        return List.of((String[]) array.getArray());
    }
}
