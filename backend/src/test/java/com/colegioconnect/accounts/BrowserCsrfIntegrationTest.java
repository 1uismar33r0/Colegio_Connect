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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.session.cookie.secure=false")
@Import(TestcontainersConfiguration.class)
class BrowserCsrfIntegrationTest {

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
    void csrfEndpointInitializesReadableCookieOverRealHttp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/auth/csrf"))
                .GET()
                .build();

        HttpResponse<Void> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.headers().allValues("Set-Cookie"))
                .anySatisfy(header -> {
                    assertThat(header).startsWith("XSRF-TOKEN=");
                    assertThat(header).contains("Path=/");
                    assertThat(header).doesNotContain("HttpOnly");
                });
    }

    @Test
    void browserCsrfAndSessionLifecycleWorksOverRealHttp() throws Exception {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

        HttpResponse<Void> initialCsrf = get(client, "/api/auth/csrf");
        assertThat(initialCsrf.statusCode()).isEqualTo(204);
        String initialToken = cookie(cookies, "XSRF-TOKEN").getValue();

        HttpResponse<Void> rejectedLogin = post(client, "/api/auth/login", loginJson(), null);
        assertThat(rejectedLogin.statusCode()).isEqualTo(403);

        HttpResponse<Void> login = post(client, "/api/auth/login", loginJson(), initialToken);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.headers().allValues(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> {
                    assertThat(header).startsWith("JSESSIONID=");
                    assertThat(header).contains("HttpOnly", "SameSite=Lax");
                });

        assertThat(get(client, "/api/auth/me").statusCode()).isEqualTo(200);

        assertThat(get(client, "/api/auth/csrf").statusCode()).isEqualTo(204);
        String authenticatedToken = cookie(cookies, "XSRF-TOKEN").getValue();
        assertThat(post(client, "/api/auth/logout", "", authenticatedToken).statusCode()).isEqualTo(204);
        assertThat(get(client, "/api/auth/me").statusCode()).isEqualTo(401);

        assertThat(get(client, "/api/auth/csrf").statusCode()).isEqualTo(204);
        assertThat(cookie(cookies, "XSRF-TOKEN").getValue()).isNotBlank();
    }

    private HttpResponse<Void> get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private HttpResponse<Void> post(HttpClient client, String path, String body, String csrfToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (csrfToken != null) {
            request.header("X-XSRF-TOKEN", csrfToken);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding());
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
