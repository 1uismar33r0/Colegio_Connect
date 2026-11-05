package com.colegioconnect.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthorizationPolicyIntegrationTest {

    @Autowired private AuthorizationPolicy authz;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private RoleAssignmentRepository roleAssignmentRepository;

    @BeforeEach
    void cleanDatabase() {
        roleAssignmentRepository.deleteAll();
        organizationRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---- Account status ----

    @Test
    void activeUserIsActive() {
        User user = createUser(UserStatus.ACTIVE);

        assertThat(authz.isActive(authenticationFor(user))).isTrue();
    }

    @Test
    void suspendedUserIsNotActive() {
        User user = createUser(UserStatus.SUSPENDED);

        assertThat(authz.isActive(authenticationFor(user))).isFalse();
    }

    @Test
    void userThatNoLongerExistsIsNotActive() {
        Authentication unknown = UsernamePasswordAuthenticationToken.authenticated(
                UUID.randomUUID().toString(), null, List.of());

        assertThat(authz.isActive(unknown)).isFalse();
    }

    // ---- Organization membership ----

    @Test
    void roleInTheSameOrganizationIsAccepted() {
        User user = createUser(UserStatus.ACTIVE);
        Organization org = createOrganization(ApprovalStatus.APPROVED);
        roleAssignmentRepository.save(new RoleAssignment(user, Role.INSTITUTIONAL, org));

        assertThat(authz.hasRoleIn(authenticationFor(user), Role.INSTITUTIONAL, org.getId())).isTrue();
    }

    @Test
    void sameRoleInAnotherOrganizationIsRejected() {
        User user = createUser(UserStatus.ACTIVE);
        Organization myOrg = createOrganization(ApprovalStatus.APPROVED);
        Organization otherOrg = createOrganization(ApprovalStatus.APPROVED);
        roleAssignmentRepository.save(new RoleAssignment(user, Role.INSTITUTIONAL, myOrg));

        assertThat(authz.hasRoleIn(authenticationFor(user), Role.INSTITUTIONAL, otherOrg.getId())).isFalse();
    }

    @Test
    void differentRoleInTheSameOrganizationIsRejected() {
        User user = createUser(UserStatus.ACTIVE);
        Organization org = createOrganization(ApprovalStatus.APPROVED);
        roleAssignmentRepository.save(new RoleAssignment(user, Role.STUDENT, org));

        assertThat(authz.hasRoleIn(authenticationFor(user), Role.INSTITUTIONAL, org.getId())).isFalse();
    }

    // ---- Organization approval ----

    @Test
    void approvedOrganizationIsApproved() {
        Organization org = createOrganization(ApprovalStatus.APPROVED);

        assertThat(authz.isApproved(org.getId())).isTrue();
    }

    @Test
    void pendingOrganizationIsNotApproved() {
        Organization org = createOrganization(ApprovalStatus.PENDING);

        assertThat(authz.isApproved(org.getId())).isFalse();
    }

    // ---- Helpers ----

    private User createUser(UserStatus status) {
        String email = UUID.randomUUID() + "@upr.edu"; // unique, so tests never collide
        User user = new User(email, "not-used-in-these-tests", AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        user.setStatus(status);
        return userRepository.save(user);
    }

    private Organization createOrganization(ApprovalStatus approvalStatus) {
        Organization org = new Organization("Test org " + UUID.randomUUID(), OrgType.UPRM_DEPARTMENT);
        org.setApprovalStatus(approvalStatus);
        return organizationRepository.save(org);
    }

    // Same shape as the login: the principal name is the user's id
    private static Authentication authenticationFor(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId().toString(), null, List.of());
    }
}