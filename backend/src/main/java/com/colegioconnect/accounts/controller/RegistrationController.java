package com.colegioconnect.accounts.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.colegioconnect.accounts.dto.RegisterRequest;
import com.colegioconnect.accounts.dto.ResendRequest;
import com.colegioconnect.accounts.dto.VerifyRequest;
import com.colegioconnect.accounts.service.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyRequest request) {
        registrationService.verify(request);
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(@Valid @RequestBody ResendRequest request) {
        registrationService.resend(request);
    }
}