package com.colegioconnect.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SessionCookieSecurityIntegrationTest {

    private static final String EMAIL = "student@upr.edu";
    private static final String PASSWORD = "a long test password";

    @Value("${local.server.port}")
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createVerifiedUser() {
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
        User user = new User(EMAIL, passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    void loginIssuesSecureSessionCookieWithProductionDefaults() throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

        HttpRequest csrfRequest = HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build();
        HttpResponse<Void> csrfResponse = client.send(csrfRequest, HttpResponse.BodyHandlers.discarding());
        assertThat(csrfResponse.statusCode()).isEqualTo(204);
        String csrfToken = cookie(cookies, "XSRF-TOKEN").getValue();

        HttpRequest loginRequest = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrfToken)
                .POST(HttpRequest.BodyPublishers.ofString(loginJson()))
                .build();
        HttpResponse<Void> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.discarding());

        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.headers().allValues(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> {
                    assertThat(header).startsWith("JSESSIONID=");
                    assertThat(header).contains("HttpOnly", "Secure", "SameSite=Lax");
                });
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static HttpCookie cookie(CookieManager cookies, String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cookie: " + name));
    }

    private static String loginJson() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}";
    }
}
