package com.colegioconnect.organizations.dto;

import java.util.UUID;

import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;

public record RoleAssignmentResponse(UUID id, UUID userId, Role role, UUID organizationId) {

    public static RoleAssignmentResponse from(RoleAssignment assignment) {
        UUID organizationId = assignment.getOrganization() == null
                ? null
                : assignment.getOrganization().getId();
        return new RoleAssignmentResponse(
                assignment.getId(), assignment.getUser().getId(), assignment.getRole(), organizationId);
    }
}