package com.colegioconnect.audit;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditEvent {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, updatable = false)
    private AuditActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false)
    private AuditTargetType targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
        // Required by JPA
    }

    AuditEvent(UUID actorId, AuditAction action, AuditTargetType targetType,
               UUID targetId, Instant occurredAt) {
        this.actorId = actorId;
        this.actorType = AuditActorType.USER;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public AuditActorType getActorType() { return actorType; }
    public AuditAction getAction() { return action; }
    public AuditTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Instant getOccurredAt() { return occurredAt; }

    static AuditEvent system(AuditAction action, AuditTargetType targetType,
                             UUID targetId, Instant occurredAt) {
        AuditEvent event = new AuditEvent(null, action, targetType, targetId, occurredAt);
        event.actorType = AuditActorType.SYSTEM;
        return event;
    }
}
