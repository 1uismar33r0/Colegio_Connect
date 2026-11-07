package com.colegioconnect.notifications;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailSender sender;

    public NotificationService(EmailSender sender) {
        this.sender = sender;
    }

    public void sendVerificationCode(String email, String code, Duration lifetime) {
        deliver("verification-code", EmailTemplates.verificationCode(email, code, lifetime));
    }
    
    private void deliver(String template, EmailMessage message) {
        String provider = sender.getClass().getSimpleName();
        String recipient = mask(message.to());
        try {
            sender.send(message);
            log.info("Email delivery SUCCESS template={} provider={} to={}", template, provider, recipient);
        } catch (RuntimeException e) {
            log.error("Email delivery FAILURE template={} provider={} to={} error={}", template, provider, recipient, e.getMessage());
            throw e;
        }
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

}
