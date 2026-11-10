package com.colegioconnect.roles;

import java.util.UUID;

import com.colegioconnect.organizations.entity.Role;

import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull Role role, UUID organizationId) {
}
