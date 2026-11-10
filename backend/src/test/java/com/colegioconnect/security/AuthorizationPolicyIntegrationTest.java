package com.colegioconnect.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthorizationPolicyIntegrationTest extends NegativeAuthorizationTestHarness {

    // ---- Account status ----

    @Test
    void activeUserIsActive() {
        User user = user(UserStatus.ACTIVE);

        assertThat(authorizationPolicy.isActive(authenticationFor(user))).isTrue();
    }

    @Test
    void suspendedUserIsNotActive() {
        User user = user(UserStatus.SUSPENDED);

        assertThat(authorizationPolicy.isActive(authenticationFor(user))).isFalse();
    }

    @Test
    void userThatNoLongerExistsIsNotActive() {
        Authentication unknown = UsernamePasswordAuthenticationToken.authenticated(
                UUID.randomUUID().toString(), null, List.of());

        assertThat(authorizationPolicy.isActive(unknown)).isFalse();
    }

    // ---- Organization membership ----

    @Test
    void roleInTheSameOrganizationIsAccepted() {
        User user = user(UserStatus.ACTIVE);
        Organization org = organization(ApprovalStatus.APPROVED);
        assign(user, Role.INSTITUTIONAL, org);

        assertThat(authorizationPolicy.hasRoleIn(
                authenticationFor(user), Role.INSTITUTIONAL, org.getId())).isTrue();
    }

    @Test
    void sameRoleInAnotherOrganizationIsRejected() {
        User user = user(UserStatus.ACTIVE);
        Organization myOrg = organization(ApprovalStatus.APPROVED);
        Organization otherOrg = organization(ApprovalStatus.APPROVED);
        assign(user, Role.INSTITUTIONAL, myOrg);

        assertThat(authorizationPolicy.hasRoleIn(
                authenticationFor(user), Role.INSTITUTIONAL, otherOrg.getId())).isFalse();
    }

    @Test
    void differentRoleInTheSameOrganizationIsRejected() {
        User user = user(UserStatus.ACTIVE);
        Organization org = organization(ApprovalStatus.APPROVED);
        assign(user, Role.STUDENT, org);

        assertThat(authorizationPolicy.hasRoleIn(
                authenticationFor(user), Role.INSTITUTIONAL, org.getId())).isFalse();
    }

    // ---- Organization approval ----

    @Test
    void approvedOrganizationIsApproved() {
        Organization org = organization(ApprovalStatus.APPROVED);

        assertThat(authorizationPolicy.isApproved(org.getId())).isTrue();
    }

    @Test
    void pendingOrganizationIsNotApproved() {
        Organization org = organization(ApprovalStatus.PENDING);

        assertThat(authorizationPolicy.isApproved(org.getId())).isFalse();
    }
}
