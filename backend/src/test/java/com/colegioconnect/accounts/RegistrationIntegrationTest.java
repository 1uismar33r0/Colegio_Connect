package com.colegioconnect.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.EmailCode;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.notifications.EmailMessage;
import com.colegioconnect.notifications.FakeEmailSender;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest(properties = "app.mail.provider=fake") // use FakeEmailSender for tests
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationIntegrationTest {

    private static final String EMAIL = "student@upr.edu";
    private static final String PASSWORD = "a long test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private RoleAssignmentRepository roleAssignmentRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FakeEmailSender emailSender;

    @BeforeEach
    void cleanDatabase() {
        roleAssignmentRepository.deleteAll();
        organizationRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
        emailSender.clear();
    }

    @Test
    void nonUprmEmailIsRejected() throws Exception {
        postJson("/api/auth/register", registerJson("someone@gmail.com"), status().isBadRequest());

        assertThat(userRepository.existsByEmail("someone@gmail.com")).isFalse();
    }

    @Test
    void registeringSendsVerificationEmail() throws Exception {
        postJson("/api/auth/register", registerJson(EMAIL), status().isCreated());

        assertThat(emailSender.getSentMessages()).singleElement().satisfies(message -> {
            assertThat(message.to()).isEqualTo(EMAIL);
            assertThat(message.subject()).containsIgnoringCase("verification code");
            assertThat(message.body()).containsPattern("\\d{6}").contains("10 minutes");
        });
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

        postJson("/api/auth/verify", verifyJson(wrongCode(code)), status().isBadRequest());

        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        registerAndGetCode();
        User user = userRepository.findByEmail(EMAIL).orElseThrow();

        // Replace the real code with one that expired a minute ago
        emailCodeRepository.deleteAll();
        emailCodeRepository.save(new EmailCode(user, passwordEncoder.encode("123456"), Instant.now().minusSeconds(60)));

        postJson("/api/auth/verify", verifyJson("123456"), status().isBadRequest());
    }

    @Test
    void reusedCodeIsRejected() throws Exception {
        String code = registerAndGetCode();

        postJson("/api/auth/verify", verifyJson(code), status().isNoContent());
        postJson("/api/auth/verify", verifyJson(code), status().isBadRequest());
    }

    @Test
    void codeIsBlockedAfterTooManyWrongAttempts() throws Exception {
        String code = registerAndGetCode();

        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/verify", verifyJson(wrongCode(code)), status().isBadRequest());
        }

        // Even the correct code is rejected now
        postJson("/api/auth/verify", verifyJson(code), status().isBadRequest());
    }

    @Test
    void resendIsLimitedRightAfterRegistering() throws Exception {
        registerAndGetCode();

        postJson("/api/auth/resend", "{\"email\":\"" + EMAIL + "\"}", status().isTooManyRequests());
    }

    // ---- Helpers ----

    private String registerAndGetCode() throws Exception {
        postJson("/api/auth/register", registerJson(EMAIL), status().isCreated());

        EmailMessage message = emailSender.getSentMessages().getLast();
        assertThat(message.to()).isEqualTo(EMAIL);

        Matcher matcher = Pattern.compile("\\d{6}").matcher(message.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    // Builds AND sends the request. Never call post(...) directly in a test: it only builds it.
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