package com.colegioconnect.notifications;

import java.time.Duration;

/**
 * Published when a verification code is created. The email is sent only after the
 * transaction that saved the code commits (see {@link VerificationEmailListener}).
 */
public record VerificationCodeRequested(String email, String code, Duration lifetime) {

    // Keep the code out of toString(), in case the event is ever logged
    @Override
    public String toString() {
        return "VerificationCodeRequested[email=" + NotificationService.mask(email) + "]";
    }
}
