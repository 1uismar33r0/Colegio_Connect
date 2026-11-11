package com.colegioconnect.accounts.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.accounts.dto.RegisterRequest;
import com.colegioconnect.accounts.dto.ResendRequest;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;

class RegistrationAllowlistTest {

    @Test
    void sandboxModeRejectsOtherRegistrationBeforeDatabaseAccess() {
        UserRepository users = mock(UserRepository.class);
        EmailCodeRepository codes = mock(EmailCodeRepository.class);
        RegistrationService service = new RegistrationService(users, codes,
                mock(PasswordEncoder.class), mock(ApplicationEventPublisher.class), true,
                "owner@upr.edu");

        assertThatThrownBy(() -> service.register(new RegisterRequest("other@upr.edu", "synthetic-pass")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                        ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(users, codes);
    }

    @Test
    void sandboxModeRejectsOtherResendBeforeDatabaseAccess() {
        UserRepository users = mock(UserRepository.class);
        EmailCodeRepository codes = mock(EmailCodeRepository.class);
        RegistrationService service = new RegistrationService(users, codes,
                mock(PasswordEncoder.class), mock(ApplicationEventPublisher.class), true,
                "OWNER@upr.edu");

        assertThatThrownBy(() -> service.resend(new ResendRequest("other@upr.edu")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                        ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(users, codes);
    }
}
