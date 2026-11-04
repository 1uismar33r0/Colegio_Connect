package com.colegioconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditEventRepository;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthorizationIntegrationTest {

    private static final String EMAIL = "authorization.student@upr.edu";
    private static final String PASSWORD = "a long test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleAssignmentRepository roleAssignmentRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private AuthorizationPolicy authorizationPolicy;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        auditEventRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        organizationRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginLoadsEveryPersistedRoleAsASpringSecurityAuthority() throws Exception {
        User user = verifiedActiveUser();
        roleAssignmentRepository.save(new RoleAssignment(user, Role.STUDENT, null));
        roleAssignmentRepository.save(new RoleAssignment(user, Role.FACULTY_OFFICE_DEPT, null));

        MockHttpSession session = login();
        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");

        assertThat(context.getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_FACULTY_OFFICE_DEPT");
    }

    @Test
    void memberCanActForTheirApprovedExternalOrganization() {
        User user = verifiedActiveUser();
        Organization organization = organizationRepository.save(
                new Organization("Approved company", OrgType.EXTERNAL_COMPANY));
        organization.setApprovalStatus(ApprovalStatus.APPROVED);
        organizationRepository.save(organization);
        roleAssignmentRepository.save(new RoleAssignment(user, Role.EXTERNAL_COMPANY, organization));

        assertThat(authorizationPolicy.isOrganizationMember(authenticationFor(user), organization.getId())).isTrue();
        assertThat(authorizationPolicy.hasApprovedOrganizationMembership(authenticationFor(user), organization.getId())).isTrue();
        assertThat(authorizationPolicy.canPublishForOrganization(authenticationFor(user), organization.getId())).isTrue();
    }

    @Test
    void memberCannotPublishForAnUnapprovedExternalOrganization() {
        User user = verifiedActiveUser();
        Organization organization = organizationRepository.save(
                new Organization("Pending company", OrgType.EXTERNAL_COMPANY));
        roleAssignmentRepository.save(new RoleAssignment(user, Role.EXTERNAL_COMPANY, organization));

        assertThat(authorizationPolicy.canPublishForOrganization(authenticationFor(user), organization.getId())).isFalse();
    }

    @Test
    void activeUserCanOnlyActAsTheirOwnOwner() {
        User user = verifiedActiveUser();

        assertThat(authorizationPolicy.isOwner(authenticationFor(user), user.getId())).isTrue();
        assertThat(authorizationPolicy.isOwner(authenticationFor(user), java.util.UUID.randomUUID())).isFalse();
    }

    @Test
    void onlyPersistedAdminModeratorCanModerateOrAssignRoles() {
        User user = verifiedActiveUser();

        assertThat(authorizationPolicy.canModerate(authenticationFor(user))).isFalse();
        assertThat(authorizationPolicy.canAssignRoles(authenticationFor(user))).isFalse();

        roleAssignmentRepository.save(new RoleAssignment(user, Role.ADMIN_MODERATOR, null));

        assertThat(authorizationPolicy.canModerate(authenticationFor(user))).isTrue();
        assertThat(authorizationPolicy.canAssignRoles(authenticationFor(user))).isTrue();
    }

    @Test
    void revokingRoleImmediatelyRemovesPermissionFromExistingAuthentication() {
        User user = verifiedActiveUser();
        RoleAssignment assignment = roleAssignmentRepository.save(
                new RoleAssignment(user, Role.ADMIN_MODERATOR, null));
        Authentication authentication = authenticationFor(user);

        assertThat(authorizationPolicy.canModerate(authentication)).isTrue();

        roleAssignmentRepository.delete(assignment);
        roleAssignmentRepository.flush();

        assertThat(authorizationPolicy.canModerate(authentication)).isFalse();
    }

    @Test
    void suspensionImmediatelyDeniesEverySharedPolicyCheck() {
        User user = verifiedActiveUser();
        Organization organization = approvedExternalOrganization("Suspended member company");
        roleAssignmentRepository.save(new RoleAssignment(user, Role.EXTERNAL_COMPANY, organization));
        roleAssignmentRepository.save(new RoleAssignment(user, Role.ADMIN_MODERATOR, null));
        Authentication authentication = authenticationFor(user);

        user.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(user);

        assertThat(authorizationPolicy.isOwner(authentication, user.getId())).isFalse();
        assertThat(authorizationPolicy.isOrganizationMember(authentication, organization.getId())).isFalse();
        assertThat(authorizationPolicy.canPublishForOrganization(authentication, organization.getId())).isFalse();
        assertThat(authorizationPolicy.canModerate(authentication)).isFalse();
        assertThat(authorizationPolicy.canAssignRoles(authentication)).isFalse();
    }

    @Test
    void organizationAssignmentDoesNotAuthorizeAnotherOrganization() {
        User user = verifiedActiveUser();
        Organization assigned = approvedExternalOrganization("Assigned company");
        Organization other = approvedExternalOrganization("Other company");
        roleAssignmentRepository.save(new RoleAssignment(user, Role.EXTERNAL_COMPANY, assigned));
        Authentication authentication = authenticationFor(user);

        assertThat(authorizationPolicy.isOrganizationMember(authentication, other.getId())).isFalse();
        assertThat(authorizationPolicy.canPublishForOrganization(authentication, other.getId())).isFalse();
    }

    @Test
    void unknownUserAndOrganizationRecordsFailClosed() {
        Authentication unknownUser = UsernamePasswordAuthenticationToken.authenticated(
                UUID.randomUUID().toString(), null, Set.of());
        UUID unknownOrganization = UUID.randomUUID();

        assertThat(authorizationPolicy.isActive(unknownUser)).isFalse();
        assertThat(authorizationPolicy.hasRole(unknownUser, Role.ADMIN_MODERATOR)).isFalse();
        assertThat(authorizationPolicy.isOwner(unknownUser, UUID.randomUUID())).isFalse();
        assertThat(authorizationPolicy.isOrganizationMember(unknownUser, unknownOrganization)).isFalse();
        assertThat(authorizationPolicy.canPublishForOrganization(unknownUser, unknownOrganization)).isFalse();
    }

    private User verifiedActiveUser() {
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Organization approvedExternalOrganization(String name) {
        Organization organization = organizationRepository.save(
                new Organization(name, OrgType.EXTERNAL_COMPANY));
        organization.setApprovalStatus(ApprovalStatus.APPROVED);
        return organizationRepository.save(organization);
    }

    private static Authentication authenticationFor(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId().toString(), null, Set.of());
    }
}
