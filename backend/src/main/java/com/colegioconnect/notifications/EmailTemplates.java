package com.colegioconnect.notifications;

import java.time.Duration;

final class EmailTemplates {
    private EmailTemplates() {
    }

    static EmailMessage verificationCode(String to, String code, Duration lifetime) {
        String body = """
            Your Colegio Connect verification code is: %s.
        
            This code will expire in %s minutes. If you did not create an account, you can ignore this email.
            """.formatted(code, lifetime.toMinutes());
        return new EmailMessage(to, "Colegio Connect Verification Code", body);
    }
}
