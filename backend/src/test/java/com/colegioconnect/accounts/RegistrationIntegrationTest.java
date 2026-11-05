package com.colegioconnect.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.EmailCode;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.notifications.EmailService;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationIntegrationTest {

    private static final String EMAIL = "student@upr.edu";
    private static final String PASSWORD = "a long test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
        @Autowired private RoleAssignmentRepository roleAssignmentRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @MockitoBean private EmailService emailService; // no real emails in tests
    
    @BeforeEach
    void cleanDatabase() {
        roleAssignmentRepository.deleteAll();
        organizationRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nonUprmEmailIsRejected() throws Exception {
        post("/api/auth/register", registerJson("someone@gmail.com"), status().isBadRequest());

        assertThat(userRepository.existsByEmail("someone@gmail.com")).isFalse();
    }

    @Test
    void correctCodeVerifiesEmail() throws Exception {
        String code = registerAndGetCode();

                postJson("/api/auth/verify", verifyJson(code), status().isNoContent());

        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void wrongCodeIsRejected() throws Exception {
        String code = registerAndGetCode();

        post("/api/auth/verify", verifyJson(wrongCode(code)), status().isBadRequest());

        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        registerAndGetCode();
        User user = userRepository.findByEmail(EMAIL).orElseThrow();

        // Replace the real code with one that expired a minute ago
        emailCodeRepository.deleteAll();
        emailCodeRepository.save(new EmailCode(user, passwordEncoder.encode("123456"), Instant.now().minusSeconds(60)));

        post("/api/auth/verify", verifyJson("123456"), status().isBadRequest());
    }

    @Test
    void reusedCodeIsRejected() throws Exception {
        String code = registerAndGetCode();

        post("/api/auth/verify", verifyJson(code), status().isNoContent());
        post("/api/auth/verify", verifyJson(code), status().isBadRequest());
    }

    @Test
    void codeIsBlockedAfterTooManyWrongAttempts() throws Exception {
        String code = registerAndGetCode();

        for (int i = 0; i < 5; i++) {
            post("/api/auth/verify", verifyJson(wrongCode(code)), status().isBadRequest());
        }

        // Even the correct code is rejected now
        post("/api/auth/verify", verifyJson(code), status().isBadRequest());
    }

    @Test
    void resendIsLimitedRightAfterRegistering() throws Exception {
        registerAndGetCode();

        post("/api/auth/resend", "{\"email\":\"" + EMAIL + "\"}", status().isTooManyRequests());
    }

    // ---- Helpers ----

    private String registerAndGetCode() throws Exception {
        postJson("/api/auth/register", registerJson(EMAIL), status().isCreated());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emailService).send(eq(EMAIL), anyString(), body.capture());

        Matcher matcher = Pattern.compile("\\d{6}").matcher(body.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private void postJson(String url, String json, ResultMatcher expectedStatus) throws Exception {
        mockMvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(expectedStatus);
    }

    private static String registerJson(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    private static String verifyJson(String code) {
        return "{\"email\":\"" + EMAIL + "\",\"code\":\"" + code + "\"}";
    }

    private static String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }
}