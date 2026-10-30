package com.colegioconnect.accounts.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.accounts.dto.RegisterRequest;
import com.colegioconnect.accounts.dto.ResendRequest;
import com.colegioconnect.accounts.dto.VerifyRequest;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.EmailCode;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.notifications.EmailService;

@Service
public class RegistrationService {

    static final String UPRM_DOMAIN = "@upr.edu";
    static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_CODES_PER_HOUR = 5;
    private static final int BCRYPT_MAX_BYTES = 72;

    private static final String INVALID_CODE = "Invalid or expired code. Request a new one.";

    private final UserRepository userRepository;
    private final EmailCodeRepository emailCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public RegistrationService(UserRepository userRepository,
                               EmailCodeRepository emailCodeRepository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.userRepository = userRepository;
        this.emailCodeRepository = emailCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalize(request.email());

        if (!email.endsWith(UPRM_DOMAIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only UPRM institutional emails (" + UPRM_DOMAIN + ") can register.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = userRepository.save(new User(email, passwordHash, AccountType.UPRM_MEMBER));

        sendNewCode(user);
    }

    // noRollbackFor: a wrong code must still count as an attempt
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void verify(VerifyRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE);
        }

        EmailCode emailCode = emailCodeRepository.findFirstByUserOrderByExpiresAtDesc(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE));

        boolean used = emailCode.getUsedAt() != null;
        boolean expired = emailCode.getExpiresAt().isBefore(Instant.now());
        boolean tooManyAttempts = emailCode.getAttempts() >= MAX_ATTEMPTS;

        if (used || expired || tooManyAttempts) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE);
        }

        if (!passwordEncoder.matches(request.code(), emailCode.getCodeHash())) {
            emailCode.addAttempt();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_CODE);
        }

        emailCode.markUsed();
        user.setEmailVerified(true);
    }

    @Transactional
    public void resend(ResendRequest request) {
        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);

        // Do nothing for unknown or verified emails, so nobody can find out which emails exist
        if (user == null || user.isEmailVerified()) {
            return;
        }

        Instant now = Instant.now();

        emailCodeRepository.findFirstByUserOrderByExpiresAtDesc(user).ifPresent(last -> {
            if (last.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Wait a minute before requesting a new code.");
            }
        });

        long codesLastHour = emailCodeRepository.countByUserAndCreatedAtAfter(user, now.minus(Duration.ofHours(1)));
        if (codesLastHour >= MAX_CODES_PER_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many codes requested. Try again later.");
        }

        sendNewCode(user);
    }

    private void sendNewCode(User user) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        emailCodeRepository.save(new EmailCode(user, codeHash, Instant.now().plus(CODE_LIFETIME)));

        emailService.send(user.getEmail(),
                "Your Colegio Connect verification code",
                "Your verification code is " + code + ". It expires in 10 minutes.");
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}