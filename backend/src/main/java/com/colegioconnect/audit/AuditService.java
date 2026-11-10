package com.colegioconnect.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, AuditAction action, AuditTargetType targetType, UUID targetId) {
        auditEventRepository.save(new AuditEvent(
                actorId, action, targetType, targetId, Instant.now()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSystem(AuditAction action, AuditTargetType targetType, UUID targetId) {
        auditEventRepository.save(AuditEvent.system(action, targetType, targetId, Instant.now()));
    }
}
