package com.colegioconnect.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

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
                .hasMessage("SMTP down");

        assertThat(output).contains("FAILURE", "SMTP down").doesNotContain("482913");
    }

    @Test
    void masksRecipient() {
        assertThat(NotificationService.mask("student@upr.edu")).isEqualTo("s***@upr.edu");
        assertThat(NotificationService.mask("broken")).isEqualTo("***");
    }
}

