package com.colegioconnect.notifications;

/**
 * An email could not be sent. Deliberately carries no cause: provider exceptions can contain
 * recipient addresses, and anything that logs this exception must not print them.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String template, String provider) {
        super("Email delivery failed (template=" + template + ", provider=" + provider + ")");
    }
}
