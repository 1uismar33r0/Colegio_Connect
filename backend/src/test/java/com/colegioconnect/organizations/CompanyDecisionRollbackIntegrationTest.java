package com.colegioconnect.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditEventRepository;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CompanyDecisionRollbackIntegrationTest {
    private static final String PASSWORD = "a long rollback test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizations;
    @Autowired private AuditEventRepository auditEvents;
    @Autowired private UserRepository users;
    @Autowired private RoleAssignmentRepository roles;
    @Autowired private EmailCodeRepository codes;
    @Autowired private PasswordEncoder passwordEncoder;
    // Only this test context replaces the audit writer, to force a failure during the business transaction.
    @MockitoBean private AuditService auditService;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        auditEvents.deleteAll();
        roles.deleteAll();
        organizations.deleteAll();
        codes.deleteAll();
        users.deleteAll();
    }

    @Test
    void auditWriteFailureRollsBackCompanyDecision() throws Exception {
        User moderator = new User("rollback.moderator@upr.edu", passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        moderator.setEmailVerified(true);
        moderator = users.save(moderator);
        roles.save(new RoleAssignment(moderator, Role.MODERATOR, null));
        Organization company = organizations.save(new Organization("Rollback Company", OrgType.EXTERNAL_COMPANY));
        MockHttpSession session = login(moderator);
        doThrow(new IllegalStateException("synthetic audit outage")).when(auditService).record(
                moderator.getId(), AuditAction.ORGANIZATION_APPROVED,
                AuditTargetType.ORGANIZATION, company.getId());

        mockMvc.perform(post("/api/admin/organizations/{id}/decision", company.getId())
                .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isInternalServerError());

        Organization unchanged = organizations.findById(company.getId()).orElseThrow();
        assertThat(unchanged.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(unchanged.getDecisionReason()).isNull();
        assertThat(auditEvents.count()).isZero();
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
