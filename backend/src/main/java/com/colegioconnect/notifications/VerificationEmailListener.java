package com.colegioconnect.notifications;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the verification email only after the user and code are committed, so nobody
 * receives a code for data that was rolled back, and SMTP latency does not hold the
 * database transaction open.
 *
 * Tradeoff: if delivery fails after the commit, the request still succeeds (201/202) and
 * the account has no delivered code. The failure is logged by NotificationService and the
 * user recovers with "Resend code" once the 60 second cooldown has passed. There is no
 * automatic retry or outbox.
 */
@Component
public class VerificationEmailListener {

    private final NotificationService notificationService;

    public VerificationEmailListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationCodeRequested(VerificationCodeRequested event) {
        try {
            notificationService.sendVerificationCode(event.email(), event.code(), event.lifetime());
        } catch (EmailDeliveryException e) {
            // Already logged with a masked address; the committed registration stays valid.
        }
    }
}
