package com.colegioconnect.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        auditEventRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void moderatorCanReadNewestAuditEventsWithoutSensitiveUserData() throws Exception {
        User moderator = createUser("audit.moderator@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.MODERATOR, null));
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

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("audit.moderator@upr.edu", "passwordHash", "verificationCode",
                        "session", "details");
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
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.MODERATOR, null));
        AuditEvent older = auditEventRepository.save(new AuditEvent(
                moderator.getId(), AuditAction.ROLE_ASSIGNED, AuditTargetType.USER,
                UUID.randomUUID(), Instant.parse("2026-11-08T01:00:00Z")));
        AuditEvent newer = auditEventRepository.save(new AuditEvent(
                moderator.getId(), AuditAction.ROLE_REVOKED, AuditTargetType.USER,
                UUID.randomUUID(), Instant.parse("2026-11-08T02:00:00Z")));

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
    void equalTimestampsHaveStableOrderAcrossPages() throws Exception {
        User moderator = createUser("audit.ties@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.MODERATOR, null));
        Instant timestamp = Instant.parse("2026-11-08T03:00:00Z");
        List<UUID> eventIds = List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"));
        for (UUID eventId : eventIds) {
            jdbcTemplate.update("""
                    INSERT INTO audit_log (id, actor_id, action, target_type, target_id, occurred_at)
                    VALUES (?, ?, 'ROLE_ASSIGNED', 'USER', ?, ?)
                    """, eventId, moderator.getId(), UUID.randomUUID(), Timestamp.from(timestamp));
        }
        MockHttpSession session = login(moderator);

        for (int page = 0; page < eventIds.size(); page++) {
            UUID expectedId = eventIds.get(eventIds.size() - 1 - page);
            mockMvc.perform(get("/api/admin/audit-events?page=" + page + "&size=1")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id").value(expectedId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(3));
        }
    }

    @Test
    void suspendingModeratorImmediatelyRevokesAuditAccessForExistingSession() throws Exception {
        User moderator = createUser("audit.suspended@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.MODERATOR, null));
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
                new RoleAssignment(moderator, Role.MODERATOR, null));
        MockHttpSession session = login(moderator);

        roleAssignmentRepository.delete(assignment);
        roleAssignmentRepository.flush();

        mockMvc.perform(get("/api/admin/audit-events").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void paginationParametersAreBounded() throws Exception {
        User moderator = createUser("audit.pagination@upr.edu");
        roleAssignmentRepository.save(new RoleAssignment(moderator, Role.MODERATOR, null));
        MockHttpSession session = login(moderator);

        mockMvc.perform(get("/api/admin/audit-events?page=-1&size=20").session(session))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-events?page=0&size=101").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void auditReadEndpointAppearsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/audit-events'].get").exists());
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
