package com.colegioconnect.notifications;

public interface EmailService {

    void send(String to, String subject, String body);
}