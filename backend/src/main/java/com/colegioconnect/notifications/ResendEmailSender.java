package com.colegioconnect.notifications;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Sends transactional plain-text mail over HTTPS on hosts where outbound SMTP is unavailable. */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {

    private static final URI API = URI.create("https://api.resend.com/emails");

    private final String apiKey;
    private final String from;
    private final URI endpoint;
    private final HttpClient client;
    private final ObjectMapper mapper;

    @Autowired
    public ResendEmailSender(@Value("${resend.api-key:}") String apiKey,
            @Value("${app.mail.from:}") String from,
            @Value("${app.registration.allowed-email:}") String allowedEmail) {
        this(apiKey, from, API, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(), allowedEmail);
    }

    ResendEmailSender(String apiKey, String from, URI endpoint, HttpClient client, ObjectMapper mapper,
            String allowedEmail) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Resend API key is required");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Resend sender address is required");
        }
        if (from.toLowerCase().endsWith("@resend.dev")
                && (allowedEmail == null || allowedEmail.isBlank())) {
            throw new IllegalArgumentException("Resend sandbox requires a registration email allowlist");
        }
        this.apiKey = apiKey;
        this.from = from;
        this.endpoint = endpoint;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public void send(EmailMessage message) {
        final String json;
        try {
            json = mapper.writeValueAsString(Map.of(
                    "from", from,
                    "to", List.of(message.to()),
                    "subject", message.subject(),
                    "text", message.body()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resend request encoding failed");
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Resend rejected email (HTTP " + response.statusCode() + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Resend request interrupted");
        } catch (IOException e) {
            throw new IllegalStateException("Resend request failed");
        }
    }
}
