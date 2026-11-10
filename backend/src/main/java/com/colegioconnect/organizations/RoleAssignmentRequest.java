package com.colegioconnect.organizations;

import java.util.UUID;

import com.colegioconnect.organizations.entity.Role;

import jakarta.validation.constraints.NotNull;

// organizationId is required for INSTITUTIONAL and must be empty for MODERATOR
public record RoleAssignmentRequest(@NotNull UUID userId, @NotNull Role role, UUID organizationId) {
}