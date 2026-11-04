package com.colegioconnect.accounts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultMatcher;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    private static final String EMAIL = "student@upr.edu";
    private static final String PASSWORD = "a long test password";
    private static final String WRONG_PASSWORD = "this is the wrong password";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void verifiedUserCanLoginAndSeeCurrentUser() throws Exception {
        createUser(true, UserStatus.ACTIVE);

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
        createUser(true, UserStatus.ACTIVE);

        login(EMAIL, WRONG_PASSWORD, status().isUnauthorized());
    }

    @Test
    void unknownEmailIsRejected() throws Exception {
        login("nobody@upr.edu", PASSWORD, status().isUnauthorized());
    }

    @Test
    void suspendedAccountIsRejected() throws Exception {
        createUser(true, UserStatus.SUSPENDED);

        login(EMAIL, PASSWORD, status().isForbidden());
    }

    @Test
    void unverifiedAccountIsRejected() throws Exception {
        createUser(false, UserStatus.ACTIVE);

        login(EMAIL, PASSWORD, status().isForbidden());
    }

    @Test
    void accountIsLockedAfterFiveWrongPasswords() throws Exception {
        createUser(true, UserStatus.ACTIVE);

        for (int i = 0; i < 5; i++) {
            login(EMAIL, WRONG_PASSWORD, status().isUnauthorized());
        }

        // correct password is rejected while locked
        login(EMAIL, PASSWORD, status().isTooManyRequests());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        createUser(true, UserStatus.ACTIVE);
        MockHttpSession session = login(EMAIL, PASSWORD, status().isOk());

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        createUser(true, UserStatus.ACTIVE);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    // ---- Helpers ----

    private void createUser(boolean emailVerified, UserStatus status) {
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(emailVerified);
        user.setStatus(status);
        userRepository.save(user);
    }

    private MockHttpSession login(String email, String password, ResultMatcher expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(expectedStatus)
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}