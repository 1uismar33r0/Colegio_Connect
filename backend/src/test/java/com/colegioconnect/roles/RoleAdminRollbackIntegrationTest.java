package com.colegioconnect.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
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
class RoleAdminRollbackIntegrationTest extends NegativeAuthorizationTestHarness {

    @MockitoBean private AuditService auditService;

    @Test
    void auditFailureRollsBackRoleAssignment() throws Exception {
        User actor = activeVerifiedUser("rollback.actor@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("rollback.target@upr.edu");
        MockHttpSession session = login(actor, status().isOk());
        doThrow(new IllegalStateException("synthetic audit outage")).when(auditService)
                .record(actor.getId(), AuditAction.ROLE_ASSIGNED, AuditTargetType.USER,
                        target.getId());

        mockMvc.perform(post("/api/admin/users/{id}/roles", target.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\",\"organizationId\":null}"))
                .andExpect(status().isInternalServerError());
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                target.getId(), Role.STUDENT)).isFalse();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void auditFailureRollsBackRoleRevocation() throws Exception {
        User actor = activeVerifiedUser("rollback.actor@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("rollback.target@upr.edu", Role.STUDENT);
        MockHttpSession session = login(actor, status().isOk());
        doThrow(new IllegalStateException("synthetic audit outage")).when(auditService)
                .record(actor.getId(), AuditAction.ROLE_REVOKED, AuditTargetType.USER,
                        target.getId());

        mockMvc.perform(delete("/api/admin/users/{id}/roles", target.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"STUDENT\",\"organizationId\":null}"))
                .andExpect(status().isInternalServerError());
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                target.getId(), Role.STUDENT)).isTrue();
        assertThat(auditEventRepository.count()).isZero();
    }
}
