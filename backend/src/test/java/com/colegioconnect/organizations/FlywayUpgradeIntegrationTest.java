package com.colegioconnect.organizations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.colegioconnect.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayUpgradeIntegrationTest {

    @Autowired private DataSource dataSource;

    @Test
    void v3IdentityDataSurvivesLaterMigrations() {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        flyway(schema, MigrationVersion.fromVersion("3")).migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID actorId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID moderatorAssignmentId = UUID.randomUUID();
        UUID companyAssignmentId = UUID.randomUUID();
        jdbc.update("INSERT INTO " + schema + ".users "
                        + "(id, email, password_hash, account_type, email_verified, status) "
                        + "VALUES (?, 'upgrade@upr.edu', 'synthetic-hash', 'UPRM_MEMBER', TRUE, 'ACTIVE')",
                actorId);
        jdbc.update("INSERT INTO " + schema + ".organizations "
                        + "(id, name, org_type, approval_status) "
                        + "VALUES (?, 'Synthetic company', 'EXTERNAL_COMPANY', 'PENDING')",
                companyId);
        jdbc.update("INSERT INTO " + schema + ".role_assignments "
                        + "(id, user_id, role) VALUES (?, ?, 'MODERATOR')",
                moderatorAssignmentId, actorId);
        jdbc.update("INSERT INTO " + schema + ".role_assignments "
                        + "(id, user_id, role, organization_id) VALUES (?, ?, 'COMPANY', ?)",
                companyAssignmentId, actorId, companyId);

        flyway(schema, null).migrate();

        Map<UUID, String> roles = jdbc.query(
                "SELECT id, role FROM " + schema + ".role_assignments",
                resultSet -> {
                    java.util.HashMap<UUID, String> found = new java.util.HashMap<>();
                    while (resultSet.next()) {
                        found.put(resultSet.getObject("id", UUID.class), resultSet.getString("role"));
                    }
                    return found;
                });
        assertThat(roles).containsExactlyInAnyOrderEntriesOf(Map.of(
                moderatorAssignmentId, "MODERATOR", companyAssignmentId, "COMPANY"));
        assertThat(jdbc.queryForObject(
                "SELECT approval_status FROM " + schema + ".organizations WHERE id = ?",
                String.class, companyId)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject(
                "SELECT decision_reason FROM " + schema + ".organizations WHERE id = ?",
                String.class, companyId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT version FROM " + schema + ".flyway_schema_history "
                        + "WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("9");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".role_admin_state WHERE id = 1",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT review_received_at FROM " + schema + ".organizations WHERE id = ?",
                java.time.OffsetDateTime.class, companyId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".listings",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".saved_listings",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".listing_responses",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".listing_reports",
                Integer.class)).isZero();

        UUID auditId = UUID.randomUUID();
        jdbc.update("INSERT INTO " + schema + ".audit_log "
                        + "(id, actor_id, action, target_type, target_id) "
                        + "VALUES (?, ?, 'ORGANIZATION_APPROVED', 'ORGANIZATION', ?)",
                auditId, actorId, companyId);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".audit_log WHERE id = ?",
                Integer.class, auditId)).isEqualTo(1);
    }

    private Flyway flyway(String schema, MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
