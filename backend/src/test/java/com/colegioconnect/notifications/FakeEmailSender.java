package com.colegioconnect.notifications;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Keeps emails in memory. Lives in test sources on purpose: it is not part of the packaged
 * application, so app.mail.provider=fake cannot silently discard mail in production.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "fake")
public class FakeEmailSender implements EmailSender {

    private final List<EmailMessage> sentMessages = new CopyOnWriteArrayList<>();
    private final AtomicReference<RuntimeException> nextFailure = new AtomicReference<>();

    @Override
    public void send(EmailMessage message) {
        RuntimeException failure = nextFailure.getAndSet(null);
        if (failure != null) {
            throw failure;
        }
        sentMessages.add(message);
    }

    /** Makes the next send throw, to simulate a provider outage. */
    public void failNext(RuntimeException failure) {
        nextFailure.set(failure);
    }

    public List<EmailMessage> getSentMessages() {
        return List.copyOf(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
        nextFailure.set(null);
    }
}
