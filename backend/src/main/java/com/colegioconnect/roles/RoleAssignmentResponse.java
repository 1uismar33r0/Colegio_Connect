package com.colegioconnect.roles;

import java.util.UUID;

import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;

public record RoleAssignmentResponse(Role role, UUID organizationId) {

    static RoleAssignmentResponse from(RoleAssignment assignment) {
        return new RoleAssignmentResponse(assignment.getRole(),
                assignment.getOrganization() == null ? null : assignment.getOrganization().getId());
    }
}
