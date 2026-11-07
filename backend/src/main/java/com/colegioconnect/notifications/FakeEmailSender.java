package com.colegioconnect.notifications;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "fake")
public class FakeEmailSender implements EmailSender {

    private final List<EmailMessage> sentMessages = new CopyOnWriteArrayList<>();

    @Override
    public void send(EmailMessage message) {
        sentMessages.add(message);
    }

    public List<EmailMessage> getSentMessages() {
        return List.copyOf(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
    }
}
