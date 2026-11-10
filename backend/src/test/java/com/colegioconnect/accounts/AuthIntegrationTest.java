package com.colegioconnect.accounts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;


import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest extends NegativeAuthorizationTestHarness {

    private static final String EMAIL = "student@upr.edu";
    private static final String PASSWORD = TEST_PASSWORD;
    private static final String WRONG_PASSWORD = "this is the wrong password";

    @Test
    void verifiedUserCanLoginAndSeeCurrentUser() throws Exception {
        activeVerifiedUser(EMAIL);

        MockHttpSession session = login(EMAIL, PASSWORD, status().isOk());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void currentUserWithoutLoginIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        activeVerifiedUser(EMAIL);

        login(EMAIL, WRONG_PASSWORD, status().isUnauthorized());
    }

    @Test
    void unknownEmailIsRejected() throws Exception {
        login("nobody@upr.edu", PASSWORD, status().isUnauthorized());
    }

    @Test
    void suspendedAccountIsRejected() throws Exception {
        user(EMAIL, UserStatus.SUSPENDED, true);

        login(EMAIL, PASSWORD, status().isForbidden());
    }

    @Test
    void unverifiedAccountIsRejected() throws Exception {
        User user = user(EMAIL, UserStatus.ACTIVE, false);

        login(user, status().isForbidden());
    }

    @Test
    void accountIsLockedAfterFiveWrongPasswords() throws Exception {
        activeVerifiedUser(EMAIL);

        for (int i = 0; i < 5; i++) {
            login(EMAIL, WRONG_PASSWORD, status().isUnauthorized());
        }

        // correct password is rejected while locked
        login(EMAIL, PASSWORD, status().isTooManyRequests());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        activeVerifiedUser(EMAIL);
        MockHttpSession session = login(EMAIL, PASSWORD, status().isOk());

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        activeVerifiedUser(EMAIL);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginLoadsTheUserRoles() throws Exception {
        User user = activeVerifiedUser(EMAIL);
        assign(user, Role.STUDENT, null);

        MockHttpSession session = login(EMAIL, PASSWORD, status().isOk());

        // The login saves the SecurityContext in the session under this key
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        assertThat(context.getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
    }
    
    @Test
    void userSuspendedAfterLoginIsBlocked() throws Exception {
        activeVerifiedUser(EMAIL);
        MockHttpSession session = login(EMAIL, PASSWORD, status().isOk());

        // A moderator suspends the account while the session is still open
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfEndpointHasNoParametersInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/csrf'].get.parameters").doesNotExist());
    }
    

}
