package com.colegioconnect.audit;

import java.util.List;

import org.springframework.data.domain.Page;

public record AuditPageResponse(
        List<AuditEventResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static AuditPageResponse from(Page<AuditEvent> events) {
        return new AuditPageResponse(
                events.getContent().stream().map(AuditEventResponse::from).toList(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages());
    }
}
