package com.colegioconnect.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(OutputCaptureExtension.class)
class NotificationServiceTest {

    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    @Test
    void successfulDeliveryIsLoggedWithoutTheCode(CapturedOutput output) {
        FakeEmailSender fake = new FakeEmailSender();

        new NotificationService(fake).sendVerificationCode("student@upr.edu", "482913", TEN_MINUTES);

        assertThat(fake.getSentMessages()).hasSize(1);
        assertThat(output).contains("SUCCESS", "s***@upr.edu").doesNotContain("482913", "student@upr.edu");
    }

    @Test
    void failedDeliveryIsLoggedAndRethrown(CapturedOutput output) {
        EmailSender broken = message -> { throw new IllegalStateException("SMTP down"); };

        assertThatThrownBy(() -> new NotificationService(broken)
                .sendVerificationCode("student@upr.edu", "482913", TEN_MINUTES))
                .isInstanceOf(EmailDeliveryException.class);

        assertThat(output).contains("FAILURE", "IllegalStateException", "SMTP down").doesNotContain("482913");
    }

    @Test
    void smtpErrorsNeverExposeFullAddresses(CapturedOutput output) {
        // Spring's MailSendException messages list the addresses that failed
        EmailSender smtp = message -> {
            throw new MailSendException("Invalid Addresses: <student@upr.edu>, other.person@upr.edu");
        };

        assertThatThrownBy(() -> new NotificationService(smtp)
                .sendVerificationCode("student@upr.edu", "482913", TEN_MINUTES))
                .isInstanceOf(EmailDeliveryException.class)
                .hasNoCause()
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("@"));

        assertThat(output)
                .contains("FAILURE", "MailSendException", "s***@upr.edu", "o***@upr.edu")
                .doesNotContain("student@upr.edu", "other.person@upr.edu", "482913");
    }

    @Test
    void masksRecipient() {
        assertThat(NotificationService.mask("student@upr.edu")).isEqualTo("s***@upr.edu");
        assertThat(NotificationService.mask("broken")).isEqualTo("***");
    }

    @Test
    void redactsEveryAddressInText() {
        assertThat(NotificationService.redact("550 <a.b@upr.edu> and x@y.com rejected"))
                .isEqualTo("550 <a***@upr.edu> and x***@y.com rejected");
        assertThat(NotificationService.redact("Connection refused")).isEqualTo("Connection refused");
        assertThat(NotificationService.redact(null)).isEmpty();
    }

    @Test
    void summaryKeepsOnlyTheFirstRedactedLine() {
        String smtpMessage = "Mail server connection failed. Failed messages: to=a.b@upr.edu\n"
                + "\tat org.example.Trace(Trace.java:1)\n";

        assertThat(NotificationService.summary(smtpMessage))
                .isEqualTo("Mail server connection failed. Failed messages: to=a***@upr.edu");
        assertThat(NotificationService.summary("x".repeat(400))).hasSize(301).endsWith("…");
        assertThat(NotificationService.summary(null)).isEmpty();
    }
}

