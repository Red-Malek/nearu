package com.nearu.backend.governorate;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class GovernorateService {

    private final JdbcClient jdbc;

    public GovernorateService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<GovernorateResponse> list() {
        return jdbc.sql("SELECT id, code, name FROM governorates ORDER BY name")
                .query((rs, i) -> new GovernorateResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("name")))
                .list();
    }
}
