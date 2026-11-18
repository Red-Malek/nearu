package com.nearu.backend.settings;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL simple (JdbcClient), comme ProviderSettingsRepository. */
@Repository
public class CustomerSettingsRepository {

    public record UserRow(String role, String status, String fullName, String phone) {
    }

    private final JdbcClient jdbc;

    public CustomerSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findUser(UUID id) {
        return jdbc.sql("SELECT role, status, full_name, phone FROM users WHERE id = :id")
                .param("id", id)
                .query((rs, i) -> new UserRow(rs.getString("role"), rs.getString("status"),
                        rs.getString("full_name"), rs.getString("phone")))
                .optional();
    }

    public boolean phoneUsedByAnother(UUID id, String phone) {
        return jdbc.sql("SELECT count(*) FROM users WHERE phone = :phone AND id <> :id")
                .param("id", id).param("phone", phone).query(Integer.class).single() > 0;
    }

    public void updateProfile(UUID id, String fullName, String phone) {
        jdbc.sql("""
                UPDATE users
                SET full_name = :name,
                    phone = COALESCE(CAST(:phone AS varchar), phone),
                    updated_at = now()
                WHERE id = :id
                """)
                .param("id", id).param("name", fullName).param("phone", phone).update();
    }

    public Optional<CustomerLocation> findLocation(UUID id) {
        return jdbc.sql("""
                SELECT address_line, postal_code, city, search_radius_km, use_gps, hide_exact_address
                FROM customer_preferences WHERE user_id = :id
                """)
                .param("id", id)
                .query((rs, i) -> new CustomerLocation(rs.getString("address_line"), rs.getString("postal_code"),
                        rs.getString("city"), rs.getInt("search_radius_km"), rs.getBoolean("use_gps"),
                        rs.getBoolean("hide_exact_address")))
                .optional();
    }

    /** Crée la ligne au premier enregistrement, la met à jour ensuite. */
    public void saveLocation(UUID id, String address, String postalCode, String city,
                             int radiusKm, boolean useGps, boolean hideExactAddress) {
        jdbc.sql("""
                INSERT INTO customer_preferences
                    (user_id, address_line, postal_code, city, search_radius_km, use_gps, hide_exact_address)
                VALUES (:id, CAST(:address AS varchar), CAST(:postal AS varchar), CAST(:city AS varchar),
                        :radius, :gps, :hide)
                ON CONFLICT (user_id) DO UPDATE SET
                    address_line = EXCLUDED.address_line,
                    postal_code = EXCLUDED.postal_code,
                    city = EXCLUDED.city,
                    search_radius_km = EXCLUDED.search_radius_km,
                    use_gps = EXCLUDED.use_gps,
                    hide_exact_address = EXCLUDED.hide_exact_address,
                    updated_at = now()
                """)
                .param("id", id).param("address", address).param("postal", postalCode).param("city", city)
                .param("radius", radiusKm).param("gps", useGps).param("hide", hideExactAddress).update();
    }

    /** Suppression douce, comme pour les prestataires : rien n'est effacé, le compte passe à DELETED. */
    public void markDeleted(UUID id) {
        jdbc.sql("UPDATE users SET status = 'DELETED', updated_at = now() WHERE id = :id")
                .param("id", id).update();
    }
}
