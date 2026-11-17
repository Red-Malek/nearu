package com.nearu.backend.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {

    private final JdbcClient jdbc;

    public UserAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Me> findActive(UUID id) {
        return jdbc.sql("SELECT id, full_name, role FROM users WHERE id = :id AND status = 'ACTIVE'")
                .param("id", id)
                .query((rs, i) -> new Me(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("role")))
                .optional();
    }

    public boolean exists(UUID id) {
        return jdbc.sql("SELECT count(*) FROM users WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single() > 0;
    }

    public void insert(UUID id, String fullName, String role) {
        jdbc.sql("INSERT INTO users (id, full_name, role) VALUES (:id, :fullName, :role)")
                .param("id", id)
                .param("fullName", fullName)
                .param("role", role)
                .update();
    }
}
