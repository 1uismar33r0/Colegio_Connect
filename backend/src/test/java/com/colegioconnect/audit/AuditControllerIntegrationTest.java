package com.colegioconnect.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

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
import org.springframework.transaction.support.TransactionTemplate;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuditControllerIntegrationTest {

    private static final String PASSWORD = "a long audit test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditService auditService;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private RoleAssignmentRepository roleAssignmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanDatabase() {
        auditEventRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void moderatorCanReadNewestAuditEventsWithoutSensitiveUserData() throws Exception {
        User moderator = createUser("audit.moderator@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.ADMIN_MODERATOR, null));
        UUID targetId = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> auditService.record(
                moderator.getId(), AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, targetId));

        MvcResult result = mockMvc.perform(get("/api/admin/audit-events")
                        .session(login(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].actorId").value(moderator.getId().toString()))
                .andExpect(jsonPath("$.items[0].action").value("ROLE_ASSIGNED"))
                .andExpect(jsonPath("$.items[0].targetType").value("USER"))
                .andExpect(jsonPath("$.items[0].targetId").value(targetId.toString()))
                .andExpect(jsonPath("$.items[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody)
                .doesNotContain("audit.moderator@upr.edu", "passwordHash", "details");
    }

    @Test
    void activeNonModeratorCannotReadAuditEvents() throws Exception {
        User user = createUser("audit.member@upr.edu");

        mockMvc.perform(get("/api/admin/audit-events").session(login(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestCannotReadAuditEvents() throws Exception {
        mockMvc.perform(get("/api/admin/audit-events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eventsArePagedNewestFirst() throws Exception {
        User moderator = createUser("audit.ordering@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.ADMIN_MODERATOR, null));
        AuditEvent older = auditEventRepository.save(new AuditEvent(
                moderator.getId(), AuditAction.ROLE_ASSIGNED, AuditTargetType.USER,
                UUID.randomUUID(), Instant.parse("2026-11-03T12:00:00Z")));
        AuditEvent newer = auditEventRepository.save(new AuditEvent(
                moderator.getId(), AuditAction.ROLE_REVOKED, AuditTargetType.USER,
                UUID.randomUUID(), Instant.parse("2026-11-03T13:00:00Z")));

        mockMvc.perform(get("/api/admin/audit-events?page=0&size=1")
                        .session(login(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(newer.getId().toString()))
                .andExpect(jsonPath("$.items[0].id").value(org.hamcrest.Matchers.not(
                        older.getId().toString())))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void suspendingModeratorImmediatelyRevokesAuditAccessForExistingSession() throws Exception {
        User moderator = createUser("audit.suspended@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.ADMIN_MODERATOR, null));
        MockHttpSession session = login(moderator);

        moderator.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(moderator);

        mockMvc.perform(get("/api/admin/audit-events").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokingModeratorRoleImmediatelyRevokesAuditAccessForExistingSession() throws Exception {
        User moderator = createUser("audit.revoked@upr.edu");
        RoleAssignment assignment = roleAssignmentRepository.save(
                new RoleAssignment(moderator, Role.ADMIN_MODERATOR, null));
        MockHttpSession session = login(moderator);

        roleAssignmentRepository.delete(assignment);
        roleAssignmentRepository.flush();

        mockMvc.perform(get("/api/admin/audit-events").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void paginationParametersAreBounded() throws Exception {
        User moderator = createUser("audit.pagination@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.ADMIN_MODERATOR, null));
        MockHttpSession session = login(moderator);

        mockMvc.perform(get("/api/admin/audit-events?page=-1&size=20").session(session))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-events?page=0&size=101").session(session))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String email) {
        User user = new User(email, passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
