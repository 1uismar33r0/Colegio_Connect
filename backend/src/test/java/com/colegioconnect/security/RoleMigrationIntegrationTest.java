package com.colegioconnect.security;

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
class RoleMigrationIntegrationTest {

    @Autowired private DataSource dataSource;

    @Test
    void v4PreservesAssignmentsWhileMappingLegacyRoleIdentifiers() {
        String schema = "role_migration_" + UUID.randomUUID().toString().replace("-", "");
        Flyway v3 = flyway(schema, MigrationVersion.fromVersion("3"));
        v3.migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        jdbc.update("INSERT INTO " + schema + ".users "
                        + "(id, email, password_hash, account_type, email_verified, status) "
                        + "VALUES (?, ?, ?, 'UPRM_MEMBER', TRUE, 'ACTIVE')",
                userId, "migration@upr.edu", "not-a-real-password-hash");
        jdbc.update("INSERT INTO " + schema + ".organizations "
                        + "(id, name, org_type, approval_status) "
                        + "VALUES (?, 'Migration organization', 'UPRM_OFFICE', 'APPROVED')",
                organizationId);

        Map<UUID, String> expectedRoles = Map.of(
                UUID.randomUUID(), "STUDENT",
                UUID.randomUUID(), "FACULTY_OFFICE_DEPT",
                UUID.randomUUID(), "EXTERNAL_COMPANY",
                UUID.randomUUID(), "ADMIN_MODERATOR");
        Map<String, String> legacyToExpected = Map.of(
                "STUDENT", "STUDENT",
                "INSTITUTIONAL", "FACULTY_OFFICE_DEPT",
                "COMPANY", "EXTERNAL_COMPANY",
                "MODERATOR", "ADMIN_MODERATOR");

        for (Map.Entry<String, String> role : legacyToExpected.entrySet()) {
            UUID assignmentId = expectedRoles.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(role.getValue()))
                    .findFirst()
                    .orElseThrow()
                    .getKey();
            jdbc.update("INSERT INTO " + schema + ".role_assignments "
                            + "(id, user_id, role, organization_id) VALUES (?, ?, ?, ?)",
                    assignmentId, userId, role.getKey(), organizationId);
        }

        flyway(schema, null).migrate();

        Map<UUID, String> migratedRoles = jdbc.query(
                "SELECT id, role FROM " + schema + ".role_assignments",
                resultSet -> {
                    java.util.HashMap<UUID, String> roles = new java.util.HashMap<>();
                    while (resultSet.next()) {
                        roles.put(resultSet.getObject("id", UUID.class), resultSet.getString("role"));
                    }
                    return roles;
                });

        assertThat(migratedRoles).containsExactlyInAnyOrderEntriesOf(expectedRoles);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".role_assignments WHERE user_id = ?",
                Integer.class, userId)).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM " + schema + ".role_assignments WHERE organization_id = ?",
                Integer.class, organizationId)).isEqualTo(4);
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
