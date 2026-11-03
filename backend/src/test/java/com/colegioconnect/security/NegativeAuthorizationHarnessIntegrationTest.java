package com.colegioconnect.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NegativeAuthorizationHarnessIntegrationTest extends NegativeAuthorizationTestHarness {

    @Test
    void unauthenticatedRequestCannotAccessAuthenticatedEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongOrganizationRoleFailsClosed() {
        User faculty = activeVerifiedUser("negative.faculty@upr.edu");
        Organization external = organization(
                "External company", OrgType.EXTERNAL_COMPANY, ApprovalStatus.APPROVED);
        assign(faculty, Role.FACULTY_OFFICE_DEPT, external);

        assertThat(authorizationPolicy.canPublishForOrganization(
                authenticationFor(faculty), external.getId())).isFalse();
    }

    @Test
    void suspendedUserCannotReuseAuthenticatedSession() throws Exception {
        User user = activeVerifiedUser("negative.suspended@upr.edu");
        MockHttpSession session = login(user);

        user.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void unverifiedUserCannotAuthenticate() throws Exception {
        User user = user("negative.unverified@upr.edu", UserStatus.ACTIVE, false);

        login(user, status().isForbidden());
    }
}
