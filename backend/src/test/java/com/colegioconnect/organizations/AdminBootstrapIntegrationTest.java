package com.colegioconnect.organizations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest(properties = "app.bootstrap.admin-email=first@upr.edu")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminBootstrapIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private AdminBootstrap adminBootstrap;

    @Test
    void configuredVerifiedUserBecomesTheFirstModerator() {
        User first = activeVerifiedUser("first@upr.edu");

        adminBootstrap.run(null);

        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                first.getId(), Role.MODERATOR)).isTrue();
        assertThat(auditEventRepository.findAll()).singleElement().satisfies(event ->
                assertThat(event.getAction()).isEqualTo(AuditAction.ROLE_ASSIGNED));
    }

    @Test
    void bootstrapOnlyWorksWhileNoModeratorExists() {
        activeVerifiedUser("existing.mod@upr.edu", Role.MODERATOR);
        User first = activeVerifiedUser("first@upr.edu");

        adminBootstrap.run(null);

        assertThat(roleAssignmentRepository.findByUserId(first.getId())).isEmpty();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void unverifiedAccountIsNotPromoted() {
        User first = user("first@upr.edu", UserStatus.ACTIVE, false);

        adminBootstrap.run(null);

        assertThat(roleAssignmentRepository.findByUserId(first.getId())).isEmpty();
    }
}