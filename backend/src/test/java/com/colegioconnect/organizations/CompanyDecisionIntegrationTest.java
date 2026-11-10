package com.colegioconnect.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditEventRepository;
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
class CompanyDecisionIntegrationTest {
    private static final String PASSWORD = "a long decision test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizations;
    @Autowired private AuditEventRepository auditEvents;
    @Autowired private UserRepository users;
    @Autowired private RoleAssignmentRepository roles;
    @Autowired private EmailCodeRepository codes;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CompanyDecisionService decisions;

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
    void moderatorApprovalCommitsStateAndAuditWithActualActorAndServerTime() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("Example Company", OrgType.EXTERNAL_COMPANY));
        Instant started = Instant.now();

        decide(company.getId(), "{\"decision\":\"APPROVED\"}", login(moderator))
                .andExpect(status().isNoContent());

        assertThat(organizations.findById(company.getId()).orElseThrow().getApprovalStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
        assertThat(auditEvents.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getActorId()).isEqualTo(moderator.getId());
            assertThat(event.getAction()).isEqualTo(AuditAction.ORGANIZATION_APPROVED);
            assertThat(event.getTargetType()).isEqualTo(AuditTargetType.ORGANIZATION);
            assertThat(event.getTargetId()).isEqualTo(company.getId());
            assertThat(event.getOccurredAt()).isBetween(started, Instant.now());
        });
    }

    @Test
    void moderatorRejectionPersistsReasonAndRejectedAuditAction() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("Rejected Company", OrgType.EXTERNAL_COMPANY));
        Instant started = Instant.now();

        decide(company.getId(), "{\"decision\":\"REJECTED\",\"reason\":\"  Business identity could not be verified.  \"}",
                login(moderator)).andExpect(status().isNoContent());

        Organization decided = organizations.findById(company.getId()).orElseThrow();
        assertThat(decided.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(decided.getDecisionReason()).isEqualTo("Business identity could not be verified.");
        assertThat(auditEvents.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getActorId()).isEqualTo(moderator.getId());
            assertThat(event.getAction()).isEqualTo(AuditAction.ORGANIZATION_REJECTED);
            assertThat(event.getTargetType()).isEqualTo(AuditTargetType.ORGANIZATION);
            assertThat(event.getTargetId()).isEqualTo(company.getId());
            assertThat(event.getOccurredAt()).isBetween(started, Instant.now());
        });
    }

    @Test
    void anonymousAndNonModeratorRequestsCannotDecide() throws Exception {
        Organization company = organizations.save(new Organization("Protected Company", OrgType.EXTERNAL_COMPANY));
        String approval = "{\"decision\":\"APPROVED\"}";
        mockMvc.perform(post("/api/admin/organizations/{id}/decision", company.getId())
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(approval))
                .andExpect(status().isUnauthorized());
        User member = createUser("decision.member@upr.edu");
        decide(company.getId(), approval, login(member)).andExpect(status().isForbidden());
        assertPendingWithoutAudit(company.getId());
    }

    @Test
    void decisionRequiresCsrfToken() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("CSRF Company", OrgType.EXTERNAL_COMPANY));
        mockMvc.perform(post("/api/admin/organizations/{id}/decision", company.getId())
                .session(login(moderator)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        assertPendingWithoutAudit(company.getId());
    }

    @Test
    void missingAndNonCompanyTargetsHaveNoAuditOrStateChange() throws Exception {
        User moderator = moderator();
        MockHttpSession session = login(moderator);
        Organization office = organizations.save(new Organization("UPRM Office", OrgType.UPRM_OFFICE));
        decide(UUID.randomUUID(), "{\"decision\":\"APPROVED\"}", session)
                .andExpect(status().isNotFound());
        decide(office.getId(), "{\"decision\":\"APPROVED\"}", session)
                .andExpect(status().isBadRequest());
        assertPendingWithoutAudit(office.getId());
    }

    @Test
    void rejectionRequiresReasonAndPendingIsNotAValidDecision() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("Invalid Decision Company", OrgType.EXTERNAL_COMPANY));
        MockHttpSession session = login(moderator);
        decide(company.getId(), "{\"decision\":\"REJECTED\",\"reason\":\"  \"}", session)
                .andExpect(status().isBadRequest());
        decide(company.getId(), "{\"decision\":\"PENDING\"}", session)
                .andExpect(status().isBadRequest());
        decide(company.getId(), "{}", session).andExpect(status().isBadRequest());
        decide(company.getId(), "{\"decision\":\"REJECTED\",\"reason\":\"" + "x".repeat(501) + "\"}", session)
                .andExpect(status().isBadRequest());
        assertPendingWithoutAudit(company.getId());
    }

    @Test
    void repeatedAndConflictingDecisionsDoNotCreateMoreAuditEvents() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("Once Company", OrgType.EXTERNAL_COMPANY));
        MockHttpSession session = login(moderator);
        decide(company.getId(), "{\"decision\":\"APPROVED\"}", session)
                .andExpect(status().isNoContent());
        decide(company.getId(), "{\"decision\":\"APPROVED\"}", session)
                .andExpect(status().isConflict());
        decide(company.getId(), "{\"decision\":\"REJECTED\",\"reason\":\"Changed mind\"}", session)
                .andExpect(status().isConflict());
        assertThat(organizations.findById(company.getId()).orElseThrow().getApprovalStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
        assertThat(auditEvents.count()).isEqualTo(1);
    }

    @Test
    void revokedModeratorRoleCannotDecideUsingExistingSession() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("Revocation Company", OrgType.EXTERNAL_COMPANY));
        MockHttpSession session = login(moderator);
        roles.deleteAll();
        roles.flush();
        decide(company.getId(), "{\"decision\":\"APPROVED\"}", session)
                .andExpect(status().isForbidden());
        assertPendingWithoutAudit(company.getId());
    }

    @Test
    void simultaneousDecisionsSerializeAndWriteOneAuditEvent() throws Exception {
        User moderator = moderator();
        Organization company = organizations.save(new Organization("Concurrent Company", OrgType.EXTERNAL_COMPANY));
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> attempt = () -> {
            start.await();
            try {
                decisions.decide(moderator.getId(), company.getId(),
                        new CompanyDecisionRequest(ApprovalStatus.APPROVED, null));
                return 204;
            } catch (ResponseStatusException exception) {
                return exception.getStatusCode().value();
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(organizations.findById(company.getId()).orElseThrow().getApprovalStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
        assertThat(auditEvents.count()).isEqualTo(1);
    }

    @Test
    void decisionEndpointAppearsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/organizations/{id}/decision'].post").exists());
    }
    private void assertPendingWithoutAudit(UUID organizationId) {
        assertThat(organizations.findById(organizationId).orElseThrow().getApprovalStatus())
                .isEqualTo(ApprovalStatus.PENDING);
        assertThat(auditEvents.count()).isZero();
    }

    private User createUser(String email) {
        User user = new User(email, passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        return users.save(user);
    }
    private ResultActions decide(UUID organizationId, String body, MockHttpSession session) throws Exception {
        return mockMvc.perform(post("/api/admin/organizations/{id}/decision", organizationId)
                .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User moderator() {
        User user = new User("decision.moderator@upr.edu", passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        user = users.save(user);
        roles.save(new RoleAssignment(user, Role.MODERATOR, null));
        return user;
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
