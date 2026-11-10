package com.colegioconnect.roles;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class RoleAdminStateRepository {

    private final JdbcTemplate jdbc;

    public RoleAdminStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lock() {
        jdbc.queryForObject("SELECT id FROM role_admin_state WHERE id = 1 FOR UPDATE",
                (resultSet, rowNumber) -> resultSet.getInt(1));
    }

    public boolean bootstrapCompleted() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT bootstrap_completed_at IS NOT NULL FROM role_admin_state WHERE id = 1",
                Boolean.class));
    }

    public void completeBootstrap(UUID userId) {
        jdbc.update("UPDATE role_admin_state SET bootstrap_completed_at = CURRENT_TIMESTAMP, "
                + "bootstrap_user_id = ? WHERE id = 1 AND bootstrap_completed_at IS NULL", userId);
    }
}
