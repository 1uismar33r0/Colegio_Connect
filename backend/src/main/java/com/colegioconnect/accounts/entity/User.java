package com.colegioconnect.accounts.entity;

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

    protected User() {
        // Required by JPA
    }

    public User(String email, String passwordHash, AccountType accountType) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountType = accountType;
        this.emailVerified = false;
        this.status = UserStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public AccountType getAccountType() { return accountType; }
    public boolean isEmailVerified() { return emailVerified; }
    public UserStatus getStatus() { return status; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setStatus(UserStatus status) { this.status = status; }
}