package com.colegioconnect.accounts.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private int failedLoginAttempts;

    private Instant lockedUntil;

    protected User() {
        // Required by JPA
    }

    public User(String email, String passwordHash, AccountType accountType) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
        this.emailVerified = false;
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public AccountType getAccountType() { return accountType; }
    public boolean isEmailVerified() { return emailVerified; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setStatus(UserStatus status) { this.status = status; }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void addFailedLogin() {
        this.failedLoginAttempts++;
    }

    public void lockUntil(Instant until) {
        this.lockedUntil = until;
        this.failedLoginAttempts = 0;
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}