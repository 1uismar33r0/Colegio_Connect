package com.colegioconnect.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.service.AdminBootstrap;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoleAssignmentIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private AdminBootstrap adminBootstrap;

    // ---- Assign ----

    @Test
    void moderatorAssignsInstitutionalRoleAndItIsAudited() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User professor = activeVerifiedUser("prof@upr.edu");
        Organization office = organization("Registrar", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);

        assign(login(moderator), professor.getId(), "INSTITUTIONAL", office.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INSTITUTIONAL"))
                .andExpect(jsonPath("$.organizationId").value(office.getId().toString()));

        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationId(
                professor.getId(), Role.INSTITUTIONAL, office.getId())).isTrue();
        assertThat(auditEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getActorId()).isEqualTo(moderator.getId());
            assertThat(event.getAction()).isEqualTo(AuditAction.ROLE_ASSIGNED);
            assertThat(event.getTargetType()).isEqualTo(AuditTargetType.USER);
            assertThat(event.getTargetId()).isEqualTo(professor.getId());
            assertThat(event.getOccurredAt()).isNotNull();
        });
    }

    @Test
    void moderatorCanAssignAnotherModerator() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User other = activeVerifiedUser("other@upr.edu");

        assign(login(moderator), other.getId(), "MODERATOR", null)
                .andExpect(status().isCreated());

        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                other.getId(), Role.MODERATOR)).isTrue();
    }

    // ---- Revoke ----

    @Test
    void moderatorRevokesRoleAndItIsAudited() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User professor = activeVerifiedUser("prof@upr.edu");
        Organization office = organization("Registrar", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        RoleAssignment assignment = assign(professor, Role.INSTITUTIONAL, office);

        mockMvc.perform(delete("/api/admin/role-assignments/{id}", assignment.getId())
                        .session(login(moderator)).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(roleAssignmentRepository.findById(assignment.getId())).isEmpty();
        assertThat(auditEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getAction()).isEqualTo(AuditAction.ROLE_REVOKED);
            assertThat(event.getActorId()).isEqualTo(moderator.getId());
            assertThat(event.getTargetId()).isEqualTo(professor.getId());
        });
    }

    @Test
    void moderatorCannotRemoveTheirOwnModeratorRole() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu");
        RoleAssignment own = assign(moderator, Role.MODERATOR, null);

        mockMvc.perform(delete("/api/admin/role-assignments/{id}", own.getId())
                        .session(login(moderator)).with(csrf()))
                .andExpect(status().isBadRequest());

        assertThat(roleAssignmentRepository.findById(own.getId())).isPresent();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void moderatorCanListRolesOfAUser() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User professor = activeVerifiedUser("prof@upr.edu");
        Organization office = organization("Registrar", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        assign(professor, Role.INSTITUTIONAL, office);

        mockMvc.perform(get("/api/admin/role-assignments").param("userId", professor.getId().toString())
                        .session(login(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("INSTITUTIONAL"));
    }

    // ---- Denials (acceptance criterion: non-admins get 403) ----

    @Test
    void nonModeratorGets403AndNothingChanges() throws Exception {
        User student = activeVerifiedUser("student@upr.edu", Role.STUDENT);
        User professor = activeVerifiedUser("prof@upr.edu");
        Organization office = organization("Registrar", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);

        assign(login(student), professor.getId(), "INSTITUTIONAL", office.getId())
                .andExpect(status().isForbidden());

        assertThat(roleAssignmentRepository.findByUserId(professor.getId())).isEmpty();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void anonymousGets401() throws Exception {
        mockMvc.perform(post("/api/admin/role-assignments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(UUID.randomUUID(), "MODERATOR", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suspendedModeratorGets403WithExistingSession() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User other = activeVerifiedUser("other@upr.edu");
        MockHttpSession session = login(moderator);

        moderator.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(moderator);

        assign(session, other.getId(), "MODERATOR", null).andExpect(status().isForbidden());
        assertThat(auditEventRepository.count()).isZero();
    }

    // ---- Invalid requests ----

    @Test
    void invalidRoleAndOrganizationCombinationsAreRejected() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User target = activeVerifiedUser("target@upr.edu");
        Organization office = organization("Registrar", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        Organization company = organization("Acme", OrgType.EXTERNAL_COMPANY, ApprovalStatus.APPROVED);
        MockHttpSession session = login(moderator);

        // institutional needs a UPRM organization
        assign(session, target.getId(), "INSTITUTIONAL", null).andExpect(status().isBadRequest());
        assign(session, target.getId(), "INSTITUTIONAL", company.getId()).andExpect(status().isBadRequest());
        // moderator is a platform role, no organization
        assign(session, target.getId(), "MODERATOR", office.getId()).andExpect(status().isBadRequest());
        // other roles are not handled by this endpoint
        assign(session, target.getId(), "STUDENT", null).andExpect(status().isBadRequest());

        assertThat(roleAssignmentRepository.findByUserId(target.getId())).isEmpty();
        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void unverifiedUserCannotReceiveARole() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User unverified = user("new@upr.edu", UserStatus.ACTIVE, false);

        assign(login(moderator), unverified.getId(), "MODERATOR", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void assigningTheSameRoleTwiceReturns409() throws Exception {
        User moderator = activeVerifiedUser("mod@upr.edu", Role.MODERATOR);
        User other = activeVerifiedUser("other@upr.edu", Role.MODERATOR);

        assign(login(moderator), other.getId(), "MODERATOR", null)
                .andExpect(status().isConflict());
        assertThat(auditEventRepository.count()).isZero();
    }

    // ---- First admin bootstrap ----

    @Test
    void bootstrapDoesNothingWhenNotConfigured() {
        activeVerifiedUser("first@upr.edu");

        adminBootstrap.run(null); // tests run without APP_BOOTSTRAP_ADMIN_EMAIL

        assertThat(roleAssignmentRepository.existsByRole(Role.MODERATOR)).isFalse();
    }

    // ---- Helpers ----

    private MockHttpSession login(User user) throws Exception {
        return login(user, status().isOk());
    }

    private ResultActions assign(MockHttpSession session, UUID userId, String role, UUID organizationId)
            throws Exception {
        return mockMvc.perform(post("/api/admin/role-assignments")
                .session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(userId, role, organizationId)));
    }

    private static String json(UUID userId, String role, UUID organizationId) {
        String org = organizationId == null ? "null" : "\"" + organizationId + "\"";
        return "{\"userId\":\"" + userId + "\",\"role\":\"" + role + "\",\"organizationId\":" + org + "}";
    }
}