package com.colegioconnect.email;

public interface EmailService {

    void send(String to, String subject, String body);
}