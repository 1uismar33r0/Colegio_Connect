package com.colegioconnect.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoleAdminIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private RoleAdminService service;

    @Test
    void moderatorSearchesExactEmailWithMinimalPagedResponse() throws Exception {
        User moderator = activeVerifiedUser("search.moderator@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("search.target@upr.edu", Role.STUDENT);
        Organization office = organization("Search office", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        assign(target, Role.INSTITUTIONAL, office);
        MockHttpSession session = login(moderator, status().isOk());

        String body = mockMvc.perform(get("/api/admin/users?email=SEARCH.TARGET@UPR.EDU&page=0&size=1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(target.getId().toString()))
                .andExpect(jsonPath("$.items[0].email").value(target.getEmail()))
                .andExpect(jsonPath("$.items[0].accountType").value("UPRM_MEMBER"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].roles[0]").value("INSTITUTIONAL"))
                .andExpect(jsonPath("$.items[0].roles[1]").value("STUDENT"))
                .andExpect(jsonPath("$.items[0].roleAssignments.length()").value(2))
                .andExpect(jsonPath("$.items[0].roleAssignments[0].organizationId")
                        .value(office.getId().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("passwordHash", "verificationCode", "session", "emailVerified");

        mockMvc.perform(get("/api/admin/users?email=search.target@upr.edu&page=1&size=1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/admin/users?email=x&size=101").session(session))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/users?email=x").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignmentAndRevocationAreAuditedAndVisibleInExistingSession() throws Exception {
        User moderator = activeVerifiedUser("roles.moderator@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("roles.target@upr.edu");
        MockHttpSession moderatorSession = login(moderator, status().isOk());
        MockHttpSession targetSession = login(target, status().isOk());
        String request = "{\"role\":\"STUDENT\",\"organizationId\":null}";

        mockMvc.perform(post("/api/admin/users/{id}/roles", target.getId())
                        .session(moderatorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(targetSession))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
        mockMvc.perform(post("/api/admin/users/{id}/roles", target.getId())
                        .session(moderatorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/admin/users/{id}/roles", target.getId())
                        .session(moderatorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(targetSession))
                .andExpect(jsonPath("$.roles.length()").value(0));

        assertThat(auditEventRepository.findAll())
                .extracting(event -> event.getAction())
                .containsExactlyInAnyOrder(AuditAction.ROLE_ASSIGNED, AuditAction.ROLE_REVOKED);
        assertThat(auditEventRepository.findAll())
                .allSatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo(moderator.getId());
                    assertThat(event.getTargetType()).isEqualTo(AuditTargetType.USER);
                    assertThat(event.getTargetId()).isEqualTo(target.getId());
                    assertThat(event.getOccurredAt()).isNotNull();
                });
    }

    @Test
    void nonModeratorsAndRevokedModeratorAreDenied() throws Exception {
        User student = activeVerifiedUser("roles.student@upr.edu", Role.STUDENT);
        User moderator = activeVerifiedUser("roles.revoked@upr.edu", Role.MODERATOR);
        MockHttpSession moderatorSession = login(moderator, status().isOk());

        mockMvc.perform(get("/api/admin/users?email=roles.student@upr.edu"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users?email=roles.student@upr.edu")
                        .session(login(student, status().isOk())))
                .andExpect(status().isForbidden());
        roleAssignmentRepository.deleteAll();
        mockMvc.perform(get("/api/admin/users?email=roles.student@upr.edu")
                        .session(moderatorSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidScopeAndMissingTargetsAreRejected() throws Exception {
        User moderator = activeVerifiedUser("validation.moderator@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("validation.target@upr.edu");
        MockHttpSession session = login(moderator, status().isOk());
        Organization company = organization("Pending company", OrgType.EXTERNAL_COMPANY, ApprovalStatus.PENDING);

        assignExpect(session, UUID.randomUUID(), "STUDENT", null, 404);
        assignExpect(session, target.getId(), "INSTITUTIONAL", null, 400);
        assignExpect(session, target.getId(), "STUDENT", company.getId(), 400);
        assignExpect(session, target.getId(), "INSTITUTIONAL", UUID.randomUUID(), 404);
        assignExpect(session, target.getId(), "INSTITUTIONAL", company.getId(), 400);
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void moderatorCannotRevokeSelfOrLastAdministrator() throws Exception {
        User moderator = activeVerifiedUser("lockout.moderator@upr.edu", Role.MODERATOR);
        User other = activeVerifiedUser("lockout.other@upr.edu", Role.MODERATOR);
        MockHttpSession session = login(moderator, status().isOk());
        String request = "{\"role\":\"MODERATOR\",\"organizationId\":null}";

        mockMvc.perform(delete("/api/admin/users/{id}/roles", moderator.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/admin/users/{id}/roles", other.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                moderator.getId(), Role.MODERATOR)).isTrue();
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void unverifiedModeratorCannotUseExistingSessionToRevokeAnotherModerator() throws Exception {
        User actor = activeVerifiedUser("last.actor@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("last.target@upr.edu", Role.MODERATOR);
        MockHttpSession session = login(actor, status().isOk());
        actor.setEmailVerified(false);
        userRepository.saveAndFlush(actor);

        mockMvc.perform(delete("/api/admin/users/{id}/roles", target.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MODERATOR\",\"organizationId\":null}"))
                .andExpect(status().isForbidden());
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                target.getId(), Role.MODERATOR)).isTrue();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void moderatorRevocationBlocksTargetsExistingSession() throws Exception {
        User actor = activeVerifiedUser("revoke.actor@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("revoke.target@upr.edu", Role.MODERATOR);
        MockHttpSession actorSession = login(actor, status().isOk());
        MockHttpSession targetSession = login(target, status().isOk());

        mockMvc.perform(delete("/api/admin/users/{id}/roles", target.getId())
                        .session(actorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MODERATOR\",\"organizationId\":null}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/users?email=revoke.target@upr.edu")
                        .session(targetSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me").session(targetSession))
                .andExpect(jsonPath("$.roles.length()").value(0));
    }

    @Test
    void roleMutationRequiresCsrfAndAnonymousIsUnauthorizedWithCsrf() throws Exception {
        User moderator = activeVerifiedUser("csrf.moderator@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("csrf.target@upr.edu");
        String request = "{\"role\":\"STUDENT\",\"organizationId\":null}";
        mockMvc.perform(post("/api/admin/users/{id}/roles", target.getId())
                        .session(login(moderator, status().isOk()))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/{id}/roles", target.getId())
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void scopedRevocationPreservesOtherMembershipsAndMissingAssignmentIs404() throws Exception {
        User moderator = activeVerifiedUser("scope.moderator@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("scope.target@upr.edu", Role.STUDENT);
        Organization first = organization("First office", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        Organization second = organization("Second office", OrgType.UPRM_DEPARTMENT,
                ApprovalStatus.APPROVED);
        assign(target, Role.INSTITUTIONAL, first);
        assign(target, Role.INSTITUTIONAL, second);
        MockHttpSession session = login(moderator, status().isOk());
        String request = "{\"role\":\"INSTITUTIONAL\",\"organizationId\":\""
                + first.getId() + "\"}";

        mockMvc.perform(delete("/api/admin/users/{id}/roles", target.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationId(
                target.getId(), Role.INSTITUTIONAL, first.getId())).isFalse();
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationId(
                target.getId(), Role.INSTITUTIONAL, second.getId())).isTrue();
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                target.getId(), Role.STUDENT)).isTrue();
        mockMvc.perform(delete("/api/admin/users/{id}/roles", target.getId())
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void companyRolesRequireApprovedCompanyAndExternalAccount() throws Exception {
        User moderator = activeVerifiedUser("company.moderator@upr.edu", Role.MODERATOR);
        User external = new User("company.employee@example.com",
                passwordEncoder.encode(TEST_PASSWORD), AccountType.EXTERNAL_ORGANIZATION);
        external.setEmailVerified(true);
        external = userRepository.save(external);
        Organization pending = organization("Pending company", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.PENDING);
        Organization approved = organization("Approved company", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.APPROVED);
        MockHttpSession session = login(moderator, status().isOk());

        assignExpect(session, external.getId(), "COMPANY", pending.getId(), 409);
        assignExpect(session, moderator.getId(), "COMPANY", approved.getId(), 400);
        assignExpect(session, external.getId(), "INSTITUTIONAL", approved.getId(), 400);
        assignExpect(session, external.getId(), "COMPANY", approved.getId(), 204);
        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationId(
                external.getId(), Role.COMPANY, approved.getId())).isTrue();
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void suspendedAndNonModeratorRolesCannotUseRoleAdministration() throws Exception {
        User suspended = user("suspended.moderator@upr.edu", UserStatus.SUSPENDED, true,
                Role.MODERATOR);
        User institutional = activeVerifiedUser("institutional@upr.edu");
        assign(institutional, Role.INSTITUTIONAL,
                organization("Office", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED));
        User company = new User("company@example.com", passwordEncoder.encode(TEST_PASSWORD),
                AccountType.EXTERNAL_ORGANIZATION);
        company.setEmailVerified(true);
        company = userRepository.save(company);
        assign(company, Role.COMPANY,
                organization("Company", OrgType.EXTERNAL_COMPANY, ApprovalStatus.APPROVED));
        String request = "{\"role\":\"STUDENT\",\"organizationId\":null}";

        for (User nonAdmin : List.of(institutional, company)) {
            MockHttpSession session = login(nonAdmin, status().isOk());
            mockMvc.perform(get("/api/admin/users?email=" + nonAdmin.getEmail()).session(session))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/users/{id}/roles", nonAdmin.getId())
                            .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isForbidden());
        }
        // A suspended moderator cannot log in, and an existing session loses access on suspension.
        suspended.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(suspended);
        MockHttpSession session = login(suspended, status().isOk());
        suspended.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(suspended);
        mockMvc.perform(get("/api/admin/users?email=institutional@upr.edu").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentDuplicateAssignmentCreatesOneRoleAndOneAuditEvent() throws Exception {
        User moderator = activeVerifiedUser("race.moderator@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("race.target@upr.edu");
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> attempt = () -> {
            start.await(10, TimeUnit.SECONDS);
            try {
                service.assign(moderator.getId(), target.getId(),
                        new RoleChangeRequest(Role.STUDENT, null));
                return 204;
            } catch (org.springframework.web.server.ResponseStatusException error) {
                return error.getStatusCode().value();
            }
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = pool.submit(attempt);
            Future<Integer> second = pool.submit(attempt);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 409);
        }
        assertThat(roleAssignmentRepository.findByUserId(target.getId()))
                .extracting(RoleAssignment::getRole).containsExactly(Role.STUDENT);
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void roleAdministrationAppearsInOpenApiWithoutAuthenticationParameter() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/users'].get").exists())
                .andExpect(jsonPath("$.paths['/api/admin/users/{id}/roles'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/users/{id}/roles'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/admin/users/{id}/roles'].post.parameters.length()")
                        .value(1));
    }

    private void assignExpect(MockHttpSession session, UUID userId, String role,
                              UUID organizationId, int statusCode) throws Exception {
        String body = "{\"role\":\"" + role + "\",\"organizationId\":"
                + (organizationId == null ? "null" : "\"" + organizationId + "\"") + "}";
        mockMvc.perform(post("/api/admin/users/{id}/roles", userId)
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(statusCode));
    }
}
