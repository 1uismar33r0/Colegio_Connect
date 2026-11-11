package com.colegioconnect.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class ResendEmailSenderTest {

    @Test
    void sendsPlainTextWithBearerTokenAndEscapedJson() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String[] authorization = new String[1];
        String[] payload = new String[1];
        server.createContext("/emails", exchange -> {
            authorization[0] = exchange.getRequestHeaders().getFirst("Authorization");
            payload[0] = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            ResendEmailSender sender = new ResendEmailSender("test-key", "verify@example.test",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/emails"),
                    HttpClient.newHttpClient(), new ObjectMapper(), "");
            sender.send(new EmailMessage("student@upr.edu", "Verify \"account\"", "Code: 123456\nExpires soon"));

            assertThat(authorization[0]).isEqualTo("Bearer test-key");
            var json = new ObjectMapper().readTree(payload[0]);
            assertThat(json.get("from").asText()).isEqualTo("verify@example.test");
            assertThat(json.get("to").get(0).asText()).isEqualTo("student@upr.edu");
            assertThat(json.get("subject").asText()).isEqualTo("Verify \"account\"");
            assertThat(json.get("text").asText()).isEqualTo("Code: 123456\nExpires soon");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsProviderFailureWithoutExposingResponseOrCode() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            byte[] body = "recipient student@upr.edu code 123456 rejected".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(422, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ResendEmailSender sender = new ResendEmailSender("test-key", "verify@example.test",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/emails"),
                    HttpClient.newHttpClient(), new ObjectMapper(), "");
            assertThatThrownBy(() -> sender.send(new EmailMessage("student@upr.edu", "Verify", "123456")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("422")
                    .hasMessageNotContaining("123456")
                    .hasMessageNotContaining("student@upr.edu");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requiresKeyAndSenderBeforeStarting() {
        assertThatThrownBy(() -> new ResendEmailSender("", "verify@example.test",
                URI.create("https://api.resend.com/emails"), HttpClient.newHttpClient(), new ObjectMapper(), ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("verify@example.test");
        assertThatThrownBy(() -> new ResendEmailSender("test-key", "",
                URI.create("https://api.resend.com/emails"), HttpClient.newHttpClient(), new ObjectMapper(), ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("test-key");
    }

    @Test
    void sandboxSenderRequiresAllowlist() {
        assertThatThrownBy(() -> new ResendEmailSender("test-key", "onboarding@resend.dev",
                URI.create("https://api.resend.com/emails"), HttpClient.newHttpClient(), new ObjectMapper(), ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("allowlist");
    }
}
