package com.colegioconnect.notifications;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Anything shaped like an email address, e.g. inside an SMTP error message
    private static final Pattern EMAIL_LIKE = Pattern.compile("[^\\s@<>\"',;:=\\[\\]()]+@[^\\s@<>\"',;:=\\[\\]()]+");

    private static final int MAX_ERROR_LENGTH = 300;

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
            // Provider errors (e.g. MailSendException) often list recipient addresses, so only a
            // redacted message is logged and the original exception is not passed on.
            log.error("Email delivery FAILURE template={} provider={} to={} error={}: {}",
                    template, provider, recipient, e.getClass().getSimpleName(), summary(e.getMessage()));
            throw new EmailDeliveryException(template, provider);
        }
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * First line of a provider error, redacted and capped. MailSendException messages embed the
     * stack traces of every failed message, which would otherwise flood the log.
     */
    static String summary(String message) {
        String redacted = redact(message).strip();
        int newline = redacted.indexOf('\n');
        String firstLine = (newline >= 0 ? redacted.substring(0, newline) : redacted).strip();
        return firstLine.length() > MAX_ERROR_LENGTH ? firstLine.substring(0, MAX_ERROR_LENGTH) + "…" : firstLine;
    }

    /** Masks every email-shaped part of a message. */
    static String redact(String text) {
        if (text == null) {
            return "";
        }
        return EMAIL_LIKE.matcher(text).replaceAll(match -> Matcher.quoteReplacement(mask(match.group())));
    }

}
