package com.colegioconnect.accounts.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_codes")
public class EmailCode {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private String codeHash;

    private Instant expiresAt;

    private Instant createdAt;

    private Instant usedAt; // null until the code is used

    private int attempts;

    protected EmailCode() {
        // Required by JPA
    }

    public EmailCode(User user, String codeHash, Instant expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.attempts = 0;

    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUsedAt() { return usedAt; }
    public int getAttempts() { return attempts; }

    public void markUsed() { this.usedAt = Instant.now(); }
    public void addAttempt() { this.attempts++; }
}