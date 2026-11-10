package com.colegioconnect.accounts.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.accounts.dto.LoginRequest;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@Service
public class LoginService {

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String INVALID_LOGIN = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final String dummyHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleAssignmentRepository roleAssignmentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleAssignmentRepository = roleAssignmentRepository;

        // this will avoid timing attacks by ensuring that the password check takes
        // a similar amount of time for both existing and non-existing users
        this.dummyHash = passwordEncoder.encode("dummy-password-used-only-for-timing");
    }

    // noRollbackFor: a wrong password must still count as a failed attempt
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);

        if (user == null) {
            // Same work as a real check, so the response time does not reveal which emails exist
            passwordEncoder.matches(request.password(), dummyHash);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_LOGIN);
        }

        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {

            user.addFailedLogin();

            if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGINS) {
                user.lockUntil(Instant.now().plus(LOCK_DURATION));
            }

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_LOGIN);
        }

        // Only checked after a correct password, so these messages are shown only to the owner
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw forbidden("ACCOUNT_SUSPENDED", "This account is suspended.");
        }
        if (!user.isEmailVerified()) {
            throw forbidden("EMAIL_NOT_VERIFIED", "Verify your email before logging in.");
        }

        user.resetFailedLogins();
        return user;
    }

    // The user's current roles, read from the database (not the session snapshot)
    @Transactional(readOnly = true)
    public List<Role> rolesFor(User user) {
        return roleAssignmentRepository.findByUserId(user.getId()).stream()
                .map(assignment -> assignment.getRole())
                .distinct()
                .sorted()
                .toList();
    }

    // Spring Security expects "ROLE_" + name, so hasRole('STUDENT') matches ROLE_STUDENT
    @Transactional(readOnly = true)
    public List<GrantedAuthority> authoritiesFor(User user) {
        return rolesFor(user).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    // Adds a stable "code" to the error so the frontend does not depend on the message text
    private static ResponseStatusException forbidden(String code, String message) {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        exception.getBody().setProperty("code", code);
        return exception;
    }
}