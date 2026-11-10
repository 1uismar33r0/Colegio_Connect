package com.colegioconnect.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminBootstrapRollbackIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private AdminBootstrapService bootstrap;
    @Autowired private RoleAdminStateRepository state;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AuditService audit;

    @BeforeEach
    void resetMarker() {
        jdbc.update("UPDATE role_admin_state SET bootstrap_completed_at = NULL, "
                + "bootstrap_user_id = NULL WHERE id = 1");
    }

    @Test
    void auditFailureRollsBackInitialGrantAndCompletionMarker() {
        User intended = activeVerifiedUser("bootstrap.rollback@upr.edu");
        doThrow(new IllegalStateException("synthetic audit outage")).when(audit)
                .recordSystem(AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, intended.getId());

        assertThatThrownBy(() -> bootstrap.bootstrap(intended.getEmail()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                intended.getId(), Role.MODERATOR)).isFalse();
        assertThat(state.bootstrapCompleted()).isFalse();
        assertThat(auditEventRepository.count()).isZero();
    }
}
