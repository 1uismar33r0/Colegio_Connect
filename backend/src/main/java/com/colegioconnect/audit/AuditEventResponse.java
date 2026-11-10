package com.colegioconnect.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorId,
        AuditAction action,
        AuditTargetType targetType,
        UUID targetId,
        Instant occurredAt) {

    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorId(),
                event.getAction(),
                event.getTargetType(),
                event.getTargetId(),
                event.getOccurredAt());
    }
}
